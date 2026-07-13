import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Verifies that a custom android.jar has a closed supertype graph: every class's direct superclass
 * and interfaces must resolve inside the jar or the running JDK. This is exactly the shape of issue
 * #100 ("Cannot access '&lt;X&gt;' which is a supertype of '&lt;Y&gt;' ... missing or conflicting
 * dependencies"). Severity is namespace-aware:
 * <ul>
 *   <li><b>HARD</b> (exit 1) when a missing supertype is in the public SDK surface the jar promises
 *       in full — {@code android/}, {@code dalvik/}, {@code java/}, {@code javax/}.</li>
 *   <li><b>soft</b> (reported, exit 0) when it is internal/impl the SDK never ships on the compile
 *       classpath ({@code com/android/ims}, {@code com/android/adservices}, protobuf, {@code libcore/},
 *       ...). {@link BuildJar} prunes these by default so a shipped jar has zero of either.</li>
 * </ul>
 *
 * <p>The prune algorithm used by {@link BuildJar} lives here too ({@link #hardMissing} +
 * {@link #soakDangling}) so verification and pruning share one definition of "resolvable".
 */
public final class ClosureVerify {

    // Missing supertypes in these namespaces are hard failures — the jar promises them in full.
    private static final String[] FAIL_PREFIXES = { "android/", "dalvik/", "java/", "javax/" };

    public static void main(String[] args) throws IOException {
        if (args.length < 1) { System.err.println("usage: ClosureVerify <jar|dir>"); System.exit(2); return; }
        Map<String, ClassNode> all = new HashMap<>();
        Path root = Paths.get(args[0]);
        if (Files.isDirectory(root)) loadDir(root, all);
        else loadJar(root, all);
        System.exit(verify(all) > 0 ? 1 : 0);
    }

    // ------------------------------------------------------------------ loading
    private static void loadJar(Path jar, Map<String, ClassNode> all) throws IOException {
        try (ZipFile z = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> e = z.entries();
            while (e.hasMoreElements()) {
                ZipEntry en = e.nextElement();
                if (!en.getName().endsWith(".class")) continue;
                try (InputStream in = z.getInputStream(en)) {
                    ClassNode cn = read(in);
                    all.put(cn.name, cn);
                }
            }
        }
    }

    private static void loadDir(Path dir, Map<String, ClassNode> all) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(p -> p.toString().endsWith(".class")).forEach(paths::add);
        }
        for (Path p : paths) {
            try (InputStream in = Files.newInputStream(p)) {
                ClassNode cn = read(in);
                all.put(cn.name, cn);
            }
        }
    }

    /** Parses a class's header only (no code/frames/debug) — all we need is super + interfaces. */
    public static ClassNode read(InputStream in) throws IOException {
        ClassNode cn = new ClassNode();
        new ClassReader(in).accept(cn, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return cn;
    }

    // ------------------------------------------------------------------ verify (gate 4)
    private static int verify(Map<String, ClassNode> all) {
        ClassLoader jdk = ClassLoader.getSystemClassLoader();
        Map<String, Boolean> jdkCache = new HashMap<>();
        List<String> hard = new ArrayList<>();
        TreeMap<String, Integer> soft = new TreeMap<>();
        for (ClassNode cn : all.values()) {
            for (String r : supers(cn)) {
                if (all.containsKey(r) || jdkLoadable(r, jdk, jdkCache)) continue;
                if (isHardNs(r)) { if (hard.size() < 50) hard.add(cn.name + "  ->  " + r); }
                else soft.merge(namespace(r), 1, Integer::sum);
            }
        }
        int softN = 0;
        for (int v : soft.values()) softN += v;
        System.err.println("[closure] scanned " + all.size() + " classes: "
                + hard.size() + " hard, " + softN + " soft (intentionally-dropped internals)");
        if (softN > 0) System.err.println("[closure] soft (ok): " + join(soft));
        if (!hard.isEmpty()) {
            System.err.println("[closure] HARD FAIL — public supertypes missing from the jar:");
            for (String s : hard) System.err.println("[closure]   " + s);
            return hard.size();
        }
        System.err.println("[closure] OK — public supertype graph is closed");
        return 0;
    }

    // ------------------------------------------------------------------ shared prune algorithm
    /**
     * Guard phase: public-surface supertypes ({@code android/dalvik/java/javax}) that are missing.
     * A non-empty result means a genuine regression — the caller must abort rather than prune, or
     * pruning would silently delete real public API and mask the issue-#100 bug.
     */
    public static List<String> hardMissing(Map<String, ClassNode> all) {
        ClassLoader jdk = ClassLoader.getSystemClassLoader();
        Map<String, Boolean> jdkCache = new HashMap<>();
        List<String> hard = new ArrayList<>();
        for (ClassNode cn : all.values()) {
            for (String r : supers(cn)) {
                if (all.containsKey(r) || jdkLoadable(r, jdk, jdkCache)) continue;
                if (isHardNs(r) && hard.size() < 50) hard.add(cn.name + "  ->  " + r);
            }
        }
        return hard;
    }

    /**
     * Well-foundedness fixpoint: the set of internal class names to remove because their supertype
     * chain is not resolvable. Only superclass/interface edges are followed (never method/field
     * references) — those are the only edges the compiler must resolve to load a type. Call only
     * after {@link #hardMissing} is empty, so every removal is rooted in an intentionally-dropped
     * namespace, never in a real public-surface hole.
     */
    public static Set<String> soakDangling(Map<String, ClassNode> all) {
        ClassLoader jdk = ClassLoader.getSystemClassLoader();
        Map<String, Boolean> jdkCache = new HashMap<>();
        Set<String> present = new HashSet<>(all.keySet());
        Set<String> removed = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            List<String> drop = new ArrayList<>();
            for (String name : present) {
                for (String r : supers(all.get(name))) {
                    if (present.contains(r) || jdkLoadable(r, jdk, jdkCache)) continue;
                    drop.add(name);
                    break;
                }
            }
            if (!drop.isEmpty()) {
                present.removeAll(drop);
                removed.addAll(drop);
                changed = true;
            }
        }
        return removed;
    }

    // ------------------------------------------------------------------ helpers
    private static List<String> supers(ClassNode cn) {
        List<String> r = new ArrayList<>();
        if (cn.superName != null) r.add(cn.superName);
        if (cn.interfaces != null) r.addAll(cn.interfaces);
        return r;
    }

    private static boolean isHardNs(String internalName) {
        for (String p : FAIL_PREFIXES) if (internalName.startsWith(p)) return true;
        return false;
    }

    private static boolean jdkLoadable(String internalName, ClassLoader jdk, Map<String, Boolean> cache) {
        Boolean b = cache.get(internalName);
        if (b != null) return b;
        boolean ok;
        try { Class.forName(internalName.replace('/', '.'), false, jdk); ok = true; }
        catch (Throwable t) { ok = false; }
        cache.put(internalName, ok);
        return ok;
    }

    private static String namespace(String internalName) {
        String[] p = internalName.split("/");
        return p.length >= 2 ? p[0] + "/" + p[1] : p[0];
    }

    public static String join(TreeMap<String, Integer> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : m.entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append(' ');
        return sb.toString().trim();
    }

    private ClosureVerify() {}
}
