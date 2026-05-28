package com.spearfix.addon;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

/**
 * SpearFix — 解碼崩潰修復 Java Agent
 *
 * 在 DecoderHandler 的 decode() 方法周圍插入 try-catch，
 * 當解碼異常時跳過當前錯誤資料包，防止客戶端斷開連線。
 *
 * 可做為 Java Agent 使用（-javaagent），也可從 addon 內部註冊。
 */
public class SpearFixAgent implements ClassFileTransformer {

    private static final String TARGET_CLASS_MAPPED    = "net/minecraft/network/handler/PacketDecoder";
    // obf name fallback for premain mode
    private static final String TARGET_CLASS_OBF       = "net/minecraft/class_2543";

    // ── Java Agent entry point ──────────────────────────────────────────
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[SpearFix] Agent loaded! Transforming DecoderHandler.decode");
        inst.addTransformer(new SpearFixAgent(), true);
    }

    // ── 供 addon 內部註冊使用 ──────────────────────────────────────────
    public static void register(Instrumentation inst) {
        inst.addTransformer(new SpearFixAgent(), true);
        System.out.println("[SpearFix] Transformer registered via Instrumentation");
    }

    // ── ClassFileTransformer ────────────────────────────────────────────
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!TARGET_CLASS_MAPPED.equals(className) && !TARGET_CLASS_OBF.equals(className)) {
            return null;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    if (loader == null) return super.getCommonSuperClass(type1, type2);
                    try {
                        Class<?> c = Class.forName(type1.replace('/', '.'), false, loader);
                        Class<?> d = Class.forName(type2.replace('/', '.'), false, loader);
                        if (c.isAssignableFrom(d)) return type1;
                        if (d.isAssignableFrom(c)) return type2;
                        if (c.isInterface() || d.isInterface()) return "java/lang/Object";
                        do { c = c.getSuperclass(); } while (!c.isAssignableFrom(d));
                        return c.getName().replace('.', '/');
                    } catch (Exception e) {
                        return "java/lang/Object";
                    }
                }
            };
            cr.accept(new FixClassVisitor(Opcodes.ASM9, cw), 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            System.err.println("[SpearFix] Failed to patch DecoderHandler:");
            t.printStackTrace();
        }
        return null;
    }

    // ── ClassVisitor ────────────────────────────────────────────────────
    private static class FixClassVisitor extends ClassVisitor {
        FixClassVisitor(int api, ClassVisitor cv) { super(api, cv); }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            // 攔截 decode(ChannelHandlerContext, ByteBuf, List) 方法
            if ("decode".equals(name) || "method_10742".equals(name)) {
                return new FixMethodVisitor(Opcodes.ASM9, mv);
            }
            return mv;
        }
    }

    // ── MethodVisitor — 對方法插入 try-catch ───────────────────────────
    private static class FixMethodVisitor extends MethodVisitor {
        private final Label startLabel   = new Label();
        private final Label endLabel     = new Label();
        private final Label handlerLabel = new Label();

        FixMethodVisitor(int api, MethodVisitor mv) { super(api, mv); }

        @Override
        public void visitCode() {
            super.visitCode();
            // 註冊 try-catch，覆蓋整個方法體
            mv.visitTryCatchBlock(startLabel, endLabel, handlerLabel, "java/lang/Throwable");
            mv.visitLabel(startLabel);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            // ── try 區塊結束 ──
            mv.visitLabel(endLabel);

            // ── catch(Throwable) ──
            mv.visitLabel(handlerLabel);

            // 把例外存入 maxLocals 槽位
            mv.visitVarInsn(Opcodes.ASTORE, maxLocals);

            // System.out.println("[SpearFix] Caught decode error, skipping packet.")
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("[SpearFix] Caught decode error, skipping packet.");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

            // 跳過緩衝區剩餘資料，避免無限重新解碼迴圈：
            //   buf.readerIndex(buf.writerIndex())
            // buf 是第 2 個區域變數（0=this, 1=ctx, 2=buf, 3=out）
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "io/netty/buffer/ByteBuf", "writerIndex", "()I", false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "io/netty/buffer/ByteBuf", "readerIndex", "(I)Lio/netty/buffer/ByteBuf;", false);
            mv.visitInsn(Opcodes.POP);

            mv.visitInsn(Opcodes.RETURN);

            // COMPUTE_FRAMES 會自動重新計算 maxStack 與 maxLocals
            super.visitMaxs(maxStack, maxLocals);
        }
    }
}
