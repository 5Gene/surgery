package osp.surger.doctor.tryfinally.test;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * @author yun.
 * @date 2022/5/28
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
public class EnhanceExceptionHandler {
    static class Victim {
        public static void hello(boolean doThrow) {
            try {
                System.out.println("in try");
                if(doThrow) {
                    throw new Exception("just for demonstration");
                }
            } catch(Exception e){
                System.out.println("in catch");
            }
        }
        static void passException(Exception e) {
            System.out.println("passException(): "+e);
        }
    }

    public static void main(String[] args)
            throws IOException, ReflectiveOperationException {

        Class<EnhanceExceptionHandler> outer = EnhanceExceptionHandler.class;
        ClassReader classReader=new ClassReader(
                outer.getResourceAsStream("EnhanceExceptionHandler$Victim.class"));
        ClassWriter classWriter=new ClassWriter(classReader,ClassWriter.COMPUTE_FRAMES);
        classReader.accept(new ClassVisitor(Opcodes.ASM5, classWriter) {
            private String className;

            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {

                className=name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {

                MethodVisitor visitor
                        = super.visitMethod(access, name, desc, signature, exceptions);
                if(name.equals("hello")) {
                    visitor=new MethodVisitor(Opcodes.ASM5, visitor) {
                        Label exceptionHandler;

                        @Override
                        public void visitLabel(Label label) {
                            super.visitLabel(label);
                            if(label==exceptionHandler) {
                                super.visitInsn(Opcodes.DUP);
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, className,
                                        "passException", "(Ljava/lang/Exception;)V", false);
                            }
                        }

                        @Override
                        public void visitTryCatchBlock(
                                Label start, Label end, Label handler, String type) {

                            exceptionHandler=handler;
                            super.visitTryCatchBlock(start, end, handler, type);
                        }
                    };
                }
                return visitor;
            }
        }, ClassReader.SKIP_FRAMES|ClassReader.SKIP_DEBUG);

        byte[] code=classWriter.toByteArray();
        Method def=ClassLoader.class.getDeclaredMethod(
                "defineClass", String.class, byte[].class, int.class, int.class);
        def.setAccessible(true);
        Class<?> instrumented=(Class<?>)def.invoke(
                outer.getClassLoader(), outer.getName()+"$Victim", code, 0, code.length);
        Method hello=instrumented.getMethod("hello", boolean.class);
        System.out.println("invoking "+hello+" with false");
        hello.invoke(null, false);
        System.out.println("invoking "+hello+" with true");
        hello.invoke(null, true);
    }
}