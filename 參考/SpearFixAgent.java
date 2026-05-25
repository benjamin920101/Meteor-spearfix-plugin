import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

/**
 * SpearFix — ViaFabricPlus 崩溃修复 Agent
 *
 * 在 DecoderHandler (class_2543) 的 decode() 方法周围插入 try-catch，
 * 当解码异常时跳过当前错误数据包，防止客户端断开连接。
 */
public class SpearFixAgent implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[SpearFix] Agent loaded! Targeting net.minecraft.class_2543 (DecoderHandler.decode)");
        inst.addTransformer(new SpearFixAgent(), true);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if ("net/minecraft/class_2543".equals(className)) {
            try {
                ClassReader cr = new ClassReader(classfileBuffer);
                // Use COMPUTE_FRAMES for Java 8+ compatibility. 
                // We override getCommonSuperClass to avoid ClassNotFoundException in some environments.
                ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES) {
                    @Override
                    protected String getCommonSuperClass(String type1, String type2) {
                        if (loader == null) return super.getCommonSuperClass(type1, type2);
                        try {
                            Class<?> c, d;
                            try {
                                c = Class.forName(type1.replace('/', '.'), false, loader);
                                d = Class.forName(type2.replace('/', '.'), false, loader);
                            } catch (Exception e) {
                                return super.getCommonSuperClass(type1, type2);
                            }
                            if (c.isAssignableFrom(d)) return type1;
                            if (d.isAssignableFrom(c)) return type2;
                            if (c.isInterface() || d.isInterface()) return "java/lang/Object";
                            do {
                                c = c.getSuperclass();
                            } while (!c.isAssignableFrom(d));
                            return c.getName().replace('.', '/');
                        } catch (Exception e) {
                            return "java/lang/Object";
                        }
                    }
                };
                cr.accept(new FixClassVisitor(Opcodes.ASM9, cw), 0);
                return cw.toByteArray();
            } catch (Throwable t) {
                System.err.println("[SpearFix] Failed to patch PacketDecoder:");
                t.printStackTrace();
            }
        }
        return null;
    }

    private static class FixClassVisitor extends ClassVisitor {
        FixClassVisitor(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            // method_10742 = intermediary name for decode(ChannelHandlerContext, ByteBuf, List)
            if ("method_10742".equals(name) || "decode".equals(name)) {
                return new FixMethodVisitor(Opcodes.ASM9, mv);
            }
            return mv;
        }
    }

    private static class FixMethodVisitor extends MethodVisitor {
        private final Label startLabel  = new Label();
        private final Label endLabel    = new Label();
        private final Label handlerLabel = new Label();

        FixMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            // Register try-catch block covering the entire method body
            mv.visitTryCatchBlock(startLabel, endLabel, handlerLabel, "java/lang/Throwable");
            mv.visitLabel(startLabel);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // End of try block
            mv.visitLabel(endLabel);

            // === Exception handler ===
            mv.visitLabel(handlerLabel);

            // Stack: [throwable]
            // Store exception in first free local var slot (index = maxLocals)
            mv.visitVarInsn(Opcodes.ASTORE, maxLocals);

            // Log: System.out.println("[SpearFix] Caught decode error, skipping packet.")
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("[SpearFix] Caught decode error, skipping packet.");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

            // Skip remaining buffer data to avoid infinite re-decode loop:
            //   in.readerIndex(in.writerIndex());
            // where 'in' is the ByteBuf parameter at local var slot 2
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "io/netty/buffer/ByteBuf", "writerIndex", "()I", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "io/netty/buffer/ByteBuf", "readerIndex", "(I)Lio/netty/buffer/ByteBuf;", false);
            mv.visitInsn(Opcodes.POP);

            mv.visitInsn(Opcodes.RETURN);
            
            // maxStack and maxLocals will be recomputed by COMPUTE_FRAMES anyway
            super.visitMaxs(maxStack, maxLocals);
        }
    }
}
