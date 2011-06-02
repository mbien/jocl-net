/*
 * Created on Wednesday, June 01 2011 02:42
 */
package com.mbien.generator;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.annotation.Out;
import com.mbien.opencl.net.remote.CLRemoteBinding;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.List;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.*;

/**
 *
 * @author Michael Bien
 */
public class ClientBindingGenerator extends NetworkBindingGenerator {

    ClientBindingGenerator(String base, String pkage, String name) {
        super(base, pkage, name);
    }

    @Override
    void generateBindingFor(Class<?> targetInterface) throws IOException {
        List<Method> methods = sortMethods(targetInterface.getMethods());
        IndentingWriter out = newWriter();
        try{
            createClientLayer(out, pkage, name, targetInterface, methods);
        }finally{
            out.close();
        }
    }

    private void createClientLayer(IndentingWriter out, String pakage, String name, Class<?> targetInterface, List<Method> methods) {

        List<?> importList = asList(
                IOException.class,
                "java.nio.*",
                "java.nio.channels.*",
                "com.jogamp.common.nio.*",
                "static com.jogamp.common.nio.NativeSizeBuffer.*",
                "static com.mbien.opencl.net.util.NetBuffers.*");

        createClassHeader(out, pakage, importList, PUBLIC|ABSTRACT, name, null, targetInterface, CLRemoteBinding.class);

        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            createClientMethodImplementation(out, i, method);
        }

        out.println();
        out.println("}");
    }


    private void createClientMethodImplementation(IndentingWriter out, int id, Method method) {

        out.indent();
        createMethodDeclaration(out, PUBLIC, true, method);
        out.indent();

        out.println();

        out.println("ByteBuffer buffer = getBuffer();");
        out.println("buffer.put((byte)3);");
        out.println("buffer.putInt("+id+");");
        out.println();

        out.println("ByteChannel channel = getChannel();");
        out.println("try {");

        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // write parameters to buffer
        for (int p = 0; p < parameterTypes.length; p++) {

            Class<?> parameter = parameterTypes[p];
            boolean in = !isAnnotatedWith(p, parameterAnnotations, Out.class);

            if(parameter.isPrimitive()) {
                if(parameter.equals(Long.TYPE)) {
                    out.println("    buffer.putLong(p"+p+");");
                }else if(parameter.equals(Integer.TYPE)) {
                    out.println("    buffer.putInt(p"+p+");");
                }else if(parameter.equals(Double.TYPE)) {
                    out.println("    buffer.putDouble(p"+p+");");
                }else if(parameter.equals(Float.TYPE)) {
                    out.println("    buffer.putFloat(p"+p+");");
                }else{
                    throw new RuntimeException();
                }
            }else{
                if(in) {
                    if(parameter.equals(IntBuffer.class)) {
                        out.println("    putInts(buffer, p"+p+");");
                    }else if(parameter.equals(NativeSizeBuffer.class)) {
                        out.println("    putBytes(buffer, p"+p+".getBuffer());");
                    }else{
                        out.println("    // ignore p"+p);
                    }
                }else{
                    out.print("    int remaining"+p+" = ");

                    if(parameter.equals(IntBuffer.class)) {
                        out.println("p"+p+"==null?"+0+":p"+p+".remaining()*4;");
                        out.println("    buffer.putInt(remaining"+p+");");
                    }else if(parameter.equals(NativeSizeBuffer.class)) {
                        out.println("p"+p+"==null?"+0+":p"+p+".remaining()*elementSize();");
                        out.println("    buffer.putInt(remaining"+p+");");
                    }else if(parameter.equals(Buffer.class)) {
                        out.println("p"+p+"==null?"+0+":p"+p+".remaining();");
                        out.println("    buffer.putInt(remaining"+p+");");
                    }else if(isStructAccessor(parameter)) {
                        out.println("p"+p+"==null?0"+":p"+p+".getBuffer().capacity();");
                        out.println("    buffer.putInt(remaining"+p+");");
                    }else{
                        out.println("0;    // ignore p"+p);
                    }
                }
            }

        }

        out.println();
        out.println("    buffer.flip();");
        out.println("    channel.write(buffer);");

        out.println();
        out.println("    buffer.rewind();");

        // read with @Out annotated parameters
        boolean read = false;
        for (int p = 0; p < parameterTypes.length; p++) {
            Class<?> parameter = parameterTypes[p];
            boolean _out = isAnnotatedWith(p, parameterAnnotations, Out.class);
            if(_out) {
                read = true;
                out.println("    if(remaining"+p+" > 0) {");
                if(parameter.equals(IntBuffer.class)) {
                    out.println("        readBuffer(channel, p"+p+", buffer);");
                    out.println("        p"+p+".rewind();");
                }else if(parameter.equals(NativeSizeBuffer.class)) {
                    out.println("        channel.read(p"+p+".getBuffer());");
                    out.println("        p"+p+".rewind();");
                }else if(parameter.equals(Buffer.class)) {
                    out.println("        channel.read((ByteBuffer)p"+p+");");
                    out.println("        p"+p+".rewind();");
                }else if(isStructAccessor(parameter)) {
                    out.println("        channel.read(p"+p+".getBuffer());");
                    out.println("        p"+p+".getBuffer().rewind();");
                }else{
                    out.println("      /* ignore */ ");
                }
                out.println("    }");
            }
        }
        if(read) {
            out.println("    buffer.rewind();");
        }

        // read and return call result value
        Class<?> returnType = method.getReturnType();
        if(returnType.isPrimitive()) {
            if(returnType.equals(Long.TYPE)) {
                out.println("    buffer.limit(8);");
                out.println("    channel.read(buffer);");
                out.println("    return buffer.getLong(0);");
            }else if(returnType.equals(Integer.TYPE)) {
                out.println("    buffer.limit(4);");
                out.println("    channel.read(buffer);");
                out.println("    return buffer.getInt(0);");
            }else if(returnType.equals(Double.TYPE)) {
                out.println("    buffer.limit(8);");
                out.println("    channel.read(buffer);");
                out.println("    return buffer.getDouble(0);");
            }else if(returnType.equals(Float.TYPE)) {
                out.println("    buffer.limit(4);");
                out.println("    channel.read(buffer);");
                out.println("    return buffer.getFloat(0);");
            }else{
                throw new RuntimeException();
            }
        }else{
            out.println("    return null;");
        }

        out.println("}catch(IOException ex) {");
        out.println("    throw new RuntimeException(ex);");
        out.println("}finally{");
        out.println("    buffer.rewind();");
        out.println("    try{if(channel != null)channel.close();}catch(IOException ex){throw new RuntimeException(ex);}");
        out.println("}");
        out.unindent();

        out.println();
        out.unindent();
        out.println("}");
    }


}
