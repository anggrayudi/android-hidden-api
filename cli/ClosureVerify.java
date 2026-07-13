import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Structural gate for a custom android.jar: every class's direct superclass and interfaces must
 * resolve either inside the jar itself or in the running JDK.
 *
 * <p>Why: this is exactly the shape of the failure in issue #100 — Android Studio / the Kotlin
 * compiler reject a jar with "Cannot access '&lt;X&gt;' which is a supertype of '&lt;Y&gt;'. Check
 * your module classpath for missing or conflicting dependencies." The stock SDK android.jar has a
 * fully closed supertype graph (zero dangling references); a merge that drops or corrupts a class
 * some other class extends breaks that invariant, and the plain "does a probe compile?" check never
 * notices because it only touches a handful of symbols.
 *
 * <p>Severity is namespace-aware, so the gate is strict where it must be and quiet where a gap is
 * intentional:
 * <ul>
 *   <li><b>HARD FAIL</b> when a missing supertype is in the public SDK surface the jar promises to
 *       provide in full — {@code android/}, {@code dalvik/}, {@code java/}, {@code javax/}. A hole
 *       here is the issue-#100 symptom (e.g. {@code android/view/View} losing its
 *       {@code android/graphics/drawable/Drawable$Callback} interface).</li>
 *   <li><b>WARN</b> when a missing supertype is internal/impl the SDK never ships on the compile
 *       classpath ({@code com/android/ims}, {@code com/android/adservices}, protobuf runtime,
 *       {@code libcore/}, dex-desugaring synthetics, ...). hiddenjar deliberately overlays only the
 *       hidden-API namespaces, so a fringe internal class reaching into dropped impl is expected.</li>
 * </ul>
 *
 * <p>Exit code: 1 if there is any hard failure, else 0.
 */
public final class ClosureVerify {

    // Missing supertypes in these namespaces are hard failures — the jar promises them in full.
    private static final String[] FAIL_PREFIXES = { "android/", "dalvik/", "java/", "javax/" };

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: ClosureVerify <jar>");
            System.exit(2);
        }
        Map<String, ClassNode> all = new HashMap<>();
        try (ZipFile z = new ZipFile(args[0])) {
            Enumeration<? extends ZipEntry> e = z.entries();
            while (e.hasMoreElements()) {
                ZipEntry en = e.nextElement();
                if (!en.getName().endsWith(".class")) continue;
                ClassNode cn = new ClassNode();
                new ClassReader(z.getInputStream(en)).accept(cn,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                all.put(cn.name, cn);
            }
        }

        ClassLoader jdk = ClassLoader.getSystemClassLoader();
        List<String> hardFails = new ArrayList<>();
        TreeMap<String, Integer> softByNs = new TreeMap<>();

        for (ClassNode cn : all.values()) {
            List<String> refs = new ArrayList<>();
            if (cn.superName != null) refs.add(cn.superName);
            if (cn.interfaces != null) refs.addAll(cn.interfaces);
            for (String r : refs) {
                if (all.containsKey(r)) continue;                 // provided by the jar
                if (loadableFromJdk(r, jdk)) continue;            // provided by the JDK
                if (isHardFail(r)) {
                    if (hardFails.size() < 50) hardFails.add(cn.name + "  ->  " + r);
                } else {
                    softByNs.merge(namespace(r), 1, Integer::sum);
                }
            }
        }

        int soft = 0;
        for (int v : softByNs.values()) soft += v;
        System.err.println("[closure] scanned " + all.size() + " classes: "
                + hardFails.size() + " hard, " + soft + " soft (intentionally-dropped internals)");
        if (soft > 0) {
            StringBuilder sb = new StringBuilder("[closure] soft (ok): ");
            for (Map.Entry<String, Integer> m : softByNs.entrySet()) {
                sb.append(m.getKey()).append('=').append(m.getValue()).append(' ');
            }
            System.err.println(sb.toString().trim());
        }
        if (!hardFails.isEmpty()) {
            System.err.println("[closure] HARD FAIL — public supertypes missing from the jar:");
            for (String s : hardFails) System.err.println("[closure]   " + s);
            System.exit(1);
        }
        System.err.println("[closure] OK — public supertype graph is closed");
    }

    private static boolean isHardFail(String internalName) {
        for (String p : FAIL_PREFIXES) if (internalName.startsWith(p)) return true;
        return false;
    }

    private static boolean loadableFromJdk(String internalName, ClassLoader jdk) {
        try {
            Class.forName(internalName.replace('/', '.'), false, jdk);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String namespace(String internalName) {
        String[] p = internalName.split("/");
        return p.length >= 2 ? p[0] + "/" + p[1] : p[0];
    }

    private ClosureVerify() {}
}
