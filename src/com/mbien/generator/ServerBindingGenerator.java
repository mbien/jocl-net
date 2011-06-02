/*
 * Created on Wednesday, June 01 2011 02:39
 */
package com.mbien.generator;


import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.CLHandler;
import com.mbien.opencl.net.annotation.Out;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.*;

/**
 *
 * @author Michael Bien
 */
public class ServerBindingGenerator extends NetworkBindingGenerator {

    ServerBindingGenerator(String base, String pkage, String name) {
        super(base, pkage, name);
    }

    @Override
    void generateBindingFor(Class<?> targetInterface) throws IOException {
        List<Method> methods = sortMethods(targetInterface.getMethods());
        IndentingWriter out = newWriter();
        try{
            createServerLayer(out, name, pkage, methods);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }finally{
            out.close();
        }
    }

    private void createServerLayer(IndentingWriter out, String name, String pkage, List<Method> methods) throws SecurityException, NoSuchMethodException {

        List<?> importList = asList(
                "com.jogamp.opencl.llb.CL",
                IOException.class,
                "java.nio.*",
                "java.nio.channels.*",
                "com.jogamp.common.nio.*",
                "static com.jogamp.common.nio.NativeSizeBuffer.*",
                "static com.jogamp.common.nio.Buffers.*",
                "static com.mbien.opencl.net.util.NetBuffers.*"
                );

        createClassHeader(out, pkage, importList, PUBLIC, name, CLHandler.class);

        //TODO remove
        out.println("    public "+name+"(CL cl) {");
        out.println("        super(cl);");
        out.println("    }");

        out.indent();

        Method method = CLHandler.class.getDeclaredMethod("handle", SocketChannel.class, Integer.TYPE);
        createMethodDeclaration(out, PUBLIC, true, method, new String[]{"channel", "methodID"}, IOException.class);

        createServerHandlerImplementation(out, methods);

        out.println();
        out.println("}");

        out.unindent();
        out.println();
        out.println("}");
    }

    private void createServerHandlerImplementation(IndentingWriter out, List<Method> methods) {

        out.indent();
        out.println();

        out.println("ByteBuffer buffer = getBuffer();");
        out.println();

        String delegate = "cl";

        out.println("switch(methodID) {");
        out.indent();
        out.println();

        for (int m = 0; m < methods.size(); m++) {
            Method method = methods.get(m);
            createServerSwitchCase(out, m, method, delegate);
        }
        out.unindent();
        out.println();
        out.println("}");

        out.unindent();

    }

    private void createServerSwitchCase(IndentingWriter out, int m, Method method, String delegate) throws RuntimeException {

        Class<?>[] parameters = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        out.println("case "+m+": {"+" // "+method.getName());
        out.indent();

        for (int p = 0; p < parameters.length; p++) {

            Class parameter = parameters[p];

            boolean in = !isAnnotatedWith(p, parameterAnnotations, Out.class);

            String type = getTypeName(parameter);
            if(Buffer.class.isAssignableFrom(parameter)) {
                type = ByteBuffer.class.getSimpleName();
            }


            if(parameter.isPrimitive()) {

                out.print(type +" p"+p +" = ");

                if(parameter.equals(Long.TYPE)) {
                    out.println("readLong(channel, buffer);");
                }else if(parameter.equals(Integer.TYPE)) {
                    out.println("readInt(channel, buffer);");
                }else if(parameter.equals(Double.TYPE)) {
                    out.println("readDouble(channel, buffer);");
                }else if(parameter.equals(Float.TYPE)) {
                    out.println("readFloat(channel, buffer);");
                }else{
                    throw new RuntimeException();
                }

            }else{
                out.println(type +" p"+p+" = null;");
                String sizeParam = "size"+p;
                
                out.print("int "+sizeParam+" = ");

                // check buffer size
                if(Buffer.class.isAssignableFrom(parameter) || parameter.equals(NativeSizeBuffer.class) || isStructAccessor(parameter)) {

                    out.println("readInt(channel, buffer);");
                    out.println("if("+sizeParam+" > 0) {");
                    out.print("    p"+p+" = ");
                    if(parameter.equals(java.nio.IntBuffer.class)) {
                        out.println("newDirectByteBuffer("+sizeParam+");");
                        if(in) {
                            out.println("    readInts(channel, p"+p+");");
                        }
                    }else if(parameter.equals(NativeSizeBuffer.class)) {
                        out.println("allocateDirect("+sizeParam+"/elementSize());");
                        if(in) {
                            out.println("    readBytes(channel, p"+p+");");
                        }
                    }else if(parameter.equals(ByteBuffer.class)) {
                        out.println("newDirectByteBuffer("+sizeParam+");");
                        if(in) {
                            out.println("    readBytes(channel, p"+p+");");
                        }
                    }else if(parameter.equals(Buffer.class)) {
                        out.println("newDirectByteBuffer("+sizeParam+");");
                        if(in) {
                            out.println("    readBytes(channel, (ByteBuffer)p"+p+");");
                        }
                    }else if(isStructAccessor(parameter)) {
                        out.println(getTypeName(parameter)+".create(newDirectByteBuffer("+sizeParam+"));");
                        if(in) {
                            out.println("    readBytes(channel, p"+p+".getBuffer());");
                        }
                    }else{
                        out.println("null;");
                    }
                    out.println("}");
                }else{
                    out.println("0; // unknown");
                }

            }
            out.println();
        }

        // method call
        Class<?> returnType = method.getReturnType();

        out.print(returnType.getSimpleName()+" ret = "+delegate+"."+method.getName()+"(");
        for (int p = 0; p < parameters.length; p++) {
            if(parameters[p].equals(IntBuffer.class)) {
                out.print("p"+p+"==null?null:p"+p+".asIntBuffer()");
            }else{
                out.print("p"+p);
            }
            if(p < parameters.length-1) {
                out.print(", ");
            }
        }
        out.println(");");

        // write @Out parameters
        for (int p = 0; p < parameters.length; p++) {
            Class<?> parameter = parameters[p];
            boolean _out = isAnnotatedWith(p, parameterAnnotations, Out.class);

            if(_out) {
                String sizeParam = "size"+p;
                out.print("if("+sizeParam+" > 0) {");
                if(parameter.equals(NativeSizeBuffer.class) || isStructAccessor(parameter)) {
                    out.print(" channel.write(p"+p+".getBuffer()); ");
                }else if(Buffer.class.isAssignableFrom(parameter)) {
                    out.print(" channel.write(p"+p+"); ");
                }else{
                    out.print("/* unknown */");
                }
                out.println("}");
            }
        }

        // write return value
        if(returnType.isPrimitive()) {
            if(returnType.equals(Long.TYPE)) {
                out.println("writeLong(channel, buffer, ret);");
            }else if(returnType.equals(Integer.TYPE)) {
                out.println("writeInt(channel, buffer, ret);");
            }else if(returnType.equals(Double.TYPE)) {
                out.println("writeDouble(channel, buffer, ret);");
            }else if(returnType.equals(Float.TYPE)) {
                out.println("writeFloat(channel, buffer, ret);");
            }else{
                throw new RuntimeException();
            }
        }
        out.println();

        out.println("break;");

        out.unindent();
        out.println();
        out.println("}");
    }

}
