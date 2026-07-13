import org.objectweb.asm.tree.ClassNode;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Assembles the custom android.jar entirely in memory: read the base SDK jar + the device's
 * dex2jar'd boot jars, overlay only the hidden-API namespaces, prune dangling classes, strip method
 * bodies to stubs, and write the output jar — without ever materialising a class file on disk.
 *
 * <p>Why in-memory: the old pipeline extracted classes to a work directory, which silently loses
 * classes whose names differ only in case (e.g. {@code android.media.AudioAttributes} vs
 * {@code android.media.Audioattributes} — both real classes in framework.jar) on a case-INSENSITIVE
 * filesystem (macOS APFS, Windows NTFS by default). A ZIP keeps both entries because they are just
 * distinct byte-string keys, so assembling straight from source zips to the output zip preserves
 * every class on every platform. It also drops the per-file copy that made the old PowerShell merge
 * slow.
 *
 * <p>The overlay is filtered by NAMESPACE, not by source jar: hidden {@code android.*} API lives in
 * APEX modules (framework-wifi, framework-bluetooth, ...) as much as in framework.jar, while the
 * JDK-shadowing ART runtime (java/*, sun/*, jdk/*, libcore/*) and repackaged libraries
 * (com.android.okhttp, com.android.org.bouncycastle, org.apache.xml*, gov.nist, ...) ship from an
 * APEX too and must stay out or the toolchain rejects the jar (issue #100). We keep android.* +
 * com.android.internal.* + dalvik.* on top of the full base SDK jar; the base already provides the
 * curated java.*, javax.*, org.*, dalvik.* public stubs.
 *
 * <p>Usage: {@code BuildJar --base <jar> --out <jar> [--keep-bodies] [--keep-dangling] <overlay-jar>...}
 * Overlay jars are applied in argument order (last writer wins). Exit 1 on a real regression (a
 * public supertype missing from the assembled set) — pruning refuses to run so it can never mask it.
 */
public final class BuildJar {

    // Overlaid from the device boot jars on top of the base SDK jar. Everything else stays out.
    private static final String[] OVERLAY_NS = { "android/", "com/android/internal/", "dalvik/" };

    public static void main(String[] args) throws IOException {
        String base = null, out = null;
        boolean keepBodies = false, keepDangling = false;
        List<String> overlays = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--base":          base = args[++i]; break;
                case "--out":           out = args[++i]; break;
                case "--keep-bodies":   keepBodies = true; break;
                case "--keep-dangling": keepDangling = true; break;
                default:                overlays.add(args[i]);
            }
        }
        if (base == null || out == null) {
            System.err.println("usage: BuildJar --base <jar> --out <jar> [--keep-bodies] [--keep-dangling] <overlay-jar>...");
            System.exit(2);
            return;
        }

        // Case-sensitive keys => two case-variant classes are two entries; nothing is ever collapsed.
        LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();
        readInto(base, entries, null);                       // base: everything except META-INF
        for (String ov : overlays) readInto(ov, entries, OVERLAY_NS); // overlay: allowlisted namespaces only

        // Prune (default): drop classes whose supertype chain is not resolvable, so the jar's graph
        // is closed like the stock android.jar. Abort instead if a PUBLIC supertype is truly missing.
        if (!keepDangling) {
            Map<String, ClassNode> nodes = new HashMap<>();
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                if (!e.getKey().endsWith(".class")) continue;
                try (InputStream in = new ByteArrayInputStream(e.getValue())) {
                    ClassNode cn = ClosureVerify.read(in);
                    nodes.put(cn.name, cn);
                }
            }
            List<String> hard = ClosureVerify.hardMissing(nodes);
            if (!hard.isEmpty()) {
                System.err.println("[build] ABORT — public supertype(s) missing; a real regression, not pruning:");
                for (String s : hard) System.err.println("[build]   " + s);
                System.exit(1);
                return;
            }
            Set<String> remove = ClosureVerify.soakDangling(nodes);   // internal names
            for (String name : remove) entries.remove(name + ".class");
            System.err.println("[build] pruned " + remove.size() + " dangling class(es) (supertype graph closed)");
        }

        // Stubify (default): replace method bodies with `throw new RuntimeException("Stub!")` so
        // Gradle's MockableJarTransform accepts the jar (issue #46). javac only reads signatures.
        if (!keepBodies) {
            int n = 0;
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                if (!e.getKey().endsWith(".class")) continue;
                try { e.setValue(Stubifier.stubify(e.getValue())); n++; }
                catch (Throwable t) { System.err.println("[build] keep-as-is (unstubbable): " + e.getKey()); }
            }
            System.err.println("[build] stubbed " + n + " class(es)");
        } else {
            System.err.println("[build] --keep-bodies: real method bodies kept; Gradle lint/unit tests will FAIL on this jar");
        }

        int classes = 0;
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(Paths.get(out))))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                ze.setTime(0L);   // deterministic, matches the SDK jar's fixed timestamps
                zos.putNextEntry(ze);
                zos.write(e.getValue());
                zos.closeEntry();
                if (e.getKey().endsWith(".class")) classes++;
            }
        }
        System.err.println("[build] wrote " + out + " (" + entries.size() + " entries, " + classes + " classes)");
    }

    /** Copies entries from {@code jar} into {@code map} (last writer wins), skipping META-INF and,
     *  when {@code nsFilter} is non-null, anything outside those path prefixes. */
    private static void readInto(String jar, Map<String, byte[]> map, String[] nsFilter) throws IOException {
        try (ZipFile z = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> e = z.entries();
            while (e.hasMoreElements()) {
                ZipEntry en = e.nextElement();
                if (en.isDirectory()) continue;
                String name = en.getName();
                if (name.startsWith("META-INF/")) continue;
                if (nsFilter != null) {
                    boolean ok = false;
                    for (String p : nsFilter) if (name.startsWith(p)) { ok = true; break; }
                    if (!ok) continue;
                }
                try (InputStream in = z.getInputStream(en)) {
                    map.put(name, in.readAllBytes());
                }
            }
        }
    }

    private BuildJar() {}
}
