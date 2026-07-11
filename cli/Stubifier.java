import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Rewrites every .class under a directory into a signature-only stub, in place.
 *
 * <p>Why: {@code hiddenjar} overlays real, dex2jar-decompiled framework bytecode onto the SDK
 * {@code android.jar}. Those real method bodies carry try/catch blocks. Gradle's
 * {@code MockableJarGenerator} (used by lint and unit tests) clears a method's instructions but
 * never clears its {@code tryCatchBlocks}; with the handler labels now dangling, its
 * {@code ClassWriter(COMPUTE_FRAMES)} throws
 * {@code Cannot read field "outgoingEdges" because "handlerRangeBlock" is null} (issue #46).
 *
 * <p>The fix is to make our overlaid classes look exactly like the SDK's own stubs: replace each
 * concrete method body with {@code throw new RuntimeException("Stub!")} and drop try/catch and
 * local-variable tables. Only method *code* is touched — signatures, generics, throws clauses,
 * annotations, constant fields, enum structure and inner-class metadata are all preserved, so
 * {@code javac} still resolves every hidden/internal symbol.
 */
public final class Stubifier {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: Stubifier <classes-dir>");
            System.exit(2);
        }
        Path root = Paths.get(args[0]);
        List<Path> classes = new ArrayList<>();
        try (Stream<Path> s = Files.walk(root)) {
            s.filter(p -> p.toString().endsWith(".class")).forEach(classes::add);
        }
        int stubbed = 0;
        int failed = 0;
        for (Path p : classes) {
            try {
                byte[] out = stubify(Files.readAllBytes(p));
                Files.write(p, out);
                stubbed++;
            } catch (Throwable t) {
                // Leave the class untouched on the (near-zero) chance ASM cannot read it.
                failed++;
                System.err.println("[stubify] WARN keep-as-is: " + root.relativize(p)
                        + " (" + t.getClass().getSimpleName() + ": " + t.getMessage() + ")");
            }
        }
        System.err.println("[stubify] classes stubbed: " + stubbed + ", failed: " + failed);
    }

    private static byte[] stubify(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassNode cn = new ClassNode();
        cr.accept(cn, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        if (cn.methods != null) {
            for (MethodNode mn : cn.methods) {
                // Abstract/native methods have no Code attribute — nothing to strip.
                if ((mn.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) continue;
                stubMethod(cn, mn);
            }
        }
        // COMPUTE_MAXS is enough: the stub body is straight-line (no branches/frames), so ASM
        // never needs getCommonSuperClass (which would try — and fail — to load framework classes).
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private static void stubMethod(ClassNode cn, MethodNode mn) {
        MethodInsnNode superCall = mn.name.equals("<init>") ? findSuperOrThisCall(cn, mn) : null;

        if (mn.tryCatchBlocks != null) mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        if (mn.visibleLocalVariableAnnotations != null) mn.visibleLocalVariableAnnotations.clear();
        if (mn.invisibleLocalVariableAnnotations != null) mn.invisibleLocalVariableAnnotations.clear();

        InsnList body = new InsnList();
        // A constructor must chain to super()/this() before returning, or the JVM verifier
        // rejects the class ("Constructor must call super() or this() before return") once it is
        // loaded — which is what happens to the *mockable* jar Gradle derives for unit tests.
        // Re-issue the original super/this call with default arguments (all mocked classes have
        // trivial ctors, so the values are never used), then fall through to the stub throw.
        if (superCall != null) {
            body.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
            for (Type arg : Type.getArgumentTypes(superCall.desc)) {
                body.add(defaultValue(arg));
            }
            body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, superCall.owner, "<init>",
                    superCall.desc, superCall.itf));
        }
        body.add(new TypeInsnNode(Opcodes.NEW, "java/lang/RuntimeException"));
        body.add(new InsnNode(Opcodes.DUP));
        body.add(new LdcInsnNode("Stub!"));
        body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException",
                "<init>", "(Ljava/lang/String;)V", false));
        body.add(new InsnNode(Opcodes.ATHROW));
        mn.instructions = body;
        // Recomputed by COMPUTE_MAXS; reset so stale values can't confuse the writer.
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    /**
     * Locates the super()/this() constructor call to preserve. A valid super call always targets
     * the direct superclass ({@code cn.superName}); a delegating constructor targets the class
     * itself ({@code cn.name}). Filtering by owner skips any {@code new Foo()} nested inside the
     * super arguments, whose {@code <init>} would otherwise be mistaken for the chain call.
     */
    private static MethodInsnNode findSuperOrThisCall(ClassNode cn, MethodNode mn) {
        MethodInsnNode thisCall = null;
        for (AbstractInsnNode insn = mn.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.INVOKESPECIAL) continue;
            MethodInsnNode call = (MethodInsnNode) insn;
            if (!call.name.equals("<init>")) continue;
            if (call.owner.equals(cn.superName)) return call;      // super(...)
            if (thisCall == null && call.owner.equals(cn.name)) thisCall = call; // this(...)
        }
        return thisCall;
    }

    private static AbstractInsnNode defaultValue(Type t) {
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:    return new InsnNode(Opcodes.ICONST_0);
            case Type.LONG:   return new InsnNode(Opcodes.LCONST_0);
            case Type.FLOAT:  return new InsnNode(Opcodes.FCONST_0);
            case Type.DOUBLE: return new InsnNode(Opcodes.DCONST_0);
            default:          return new InsnNode(Opcodes.ACONST_NULL); // object / array
        }
    }

    private Stubifier() {}
}
