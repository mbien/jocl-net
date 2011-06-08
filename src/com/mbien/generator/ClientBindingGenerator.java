/*
 * Created on Wednesday, June 01 2011 02:42
 */
package com.mbien.generator;

import com.jogamp.common.nio.NativeBuffer;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.annotation.Out;
import com.mbien.opencl.net.remote.CLRemoteBinding;
import com.mbien.opencl.net.util.NetBuffers;
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

    private final byte BINDING_ID;

    ClientBindingGenerator(String base, String pkage, String name, byte id) {
        super(base, pkage, name);
        this.BINDING_ID = id;
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
                "static "+NativeSizeBuffer.class.getCanonicalName()+".*",
                "static "+NetBuffers.class.getCanonicalName()+".*");

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
        out.println("buffer.put((byte)"+BINDING_ID+");");
        out.println("buffer.putInt("+id+");");
        out.println();

        out.println("ByteChannel channel = getChannel();");
        out.println("try {");

        out.indent();

        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // write parameters to buffer
        for (int p = 0; p < parameterTypes.length; p++) {

            Class<?> parameter = parameterTypes[p];
            boolean in = !isAnnotatedWith(p, parameterAnnotations, Out.class);
            createWriteParameterSection(out, "p"+p, parameter, p, in);

        }

        out.println();
        out.println("buffer.flip();");
        out.println("channel.write(buffer);");

        out.println();
        out.println("buffer.rewind();");

        // read with @Out annotated parameters
        boolean read = false;
        for (int p = 0; p < parameterTypes.length; p++) {
            Class<?> parameter = parameterTypes[p];
            if(parameter.isArray()) {
                out.println("  // ignore p"+p);
                continue;
            }
            boolean _out = isAnnotatedWith(p, parameterAnnotations, Out.class);
            if(_out) {
                read = true;
                out.println("if(remaining"+p+" > 0) {");
                if(parameter.equals(IntBuffer.class)) {
                    out.println("    readBuffer(channel, p"+p+", buffer);");
                    out.println("    p"+p+".rewind();");
                }else if(NativeBuffer.class.isAssignableFrom(parameter)) {
                    out.println("    channel.read(p"+p+".getBuffer());");
                    out.println("    p"+p+".rewind();");
                }else if(parameter.equals(Buffer.class)) {
                    out.println("    channel.read((ByteBuffer)p"+p+");");
                    out.println("    p"+p+".rewind();");
                }else if(isStructAccessor(parameter)) {
                    out.println("    channel.read(p"+p+".getBuffer());");
                    out.println("    p"+p+".getBuffer().rewind();");
                }else{
                    out.println("  /* ignore */ ");
                }
                out.println("}");
            }
        }
        if(read) {
            out.println("buffer.rewind();");
        }

        // read and return call result value
        Class<?> returnType = method.getReturnType();
        if(returnType.isPrimitive()) {
            if(returnType.equals(long.class)) {
                out.println("buffer.limit(8);");
                out.println("channel.read(buffer);");
                out.println("return buffer.getLong(0);");
            }else if(returnType.equals(int.class)) {
                out.println("buffer.limit(4);");
                out.println("channel.read(buffer);");
                out.println("return buffer.getInt(0);");
            }else if(returnType.equals(double.class)) {
                out.println("buffer.limit(8);");
                out.println("channel.read(buffer);");
                out.println("return buffer.getDouble(0);");
            }else if(returnType.equals(float.class)) {
                out.println("buffer.limit(4);");
                out.println("channel.read(buffer);");
                out.println("return buffer.getFloat(0);");
            }else if(returnType.equals(void.class)) {
                // nothing to do
            }else{
                throw new UnsupportedOperationException("unexpected type: "+returnType.getCanonicalName());
            }
        }else{
            out.println("return null;");
        }

        out.unindent();
        out.println();

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

    private void createWriteParameterSection(IndentingWriter out, String paramName, Class<?> parameter, int p, boolean in) throws RuntimeException {

        if(parameter.isPrimitive()) {
            if(parameter.equals(long.class)) {
                out.println("buffer.putLong("+paramName+");");
            }else if(parameter.equals(int.class)) {
                out.println("buffer.putInt("+paramName+");");
            }else if(parameter.equals(double.class)) {
                out.println("buffer.putDouble("+paramName+");");
            }else if(parameter.equals(float.class)) {
                out.println("buffer.putFloat("+paramName+");");
            }else{
                throw new RuntimeException();
            }
        }else if(parameter.isArray()) {
            Class<?> component = parameter.getComponentType();

            if(component.isPrimitive()) {
                out.println("// ignore "+paramName);
                return;
            }

            out.println("if("+paramName+"==null) {");
            out.println("    buffer.putInt(-1);");
            out.println("}else{");
            out.indent();
            out.println("buffer.putInt("+paramName+".length);");
            out.println("for(int i = 0; i < "+paramName+".length; i++) {");
            out.indent();
            createWriteParameterSection(out, paramName+"[i]", component, p, in);
            out.unindent();
            out.println();
            out.unindent();
            out.println("}");
            out.println("}");
        }else{
            if(in) {
                if(parameter.equals(IntBuffer.class)) {
                    out.println("putInts(buffer, "+paramName+");");
                }else if(NativeBuffer.class.isAssignableFrom(parameter)) {
                    out.println("putBytes(buffer, "+paramName+");");
                }else if(parameter.equals(String.class)) {
                    out.println("putString(buffer, "+paramName+");");
                }else{
                    out.println("// ignore "+paramName);
                }
            }else{
                out.print("int remaining"+p+" = ");

                if(parameter.equals(IntBuffer.class)) {
                    out.println(paramName+"==null?"+0+":"+paramName+".remaining()*4;");
                    out.println("buffer.putInt(remaining"+p+");");
                }else if(parameter.equals(NativeSizeBuffer.class)) {
                    out.println(paramName+"==null?"+0+":"+paramName+".remaining()*elementSize();");
                    out.println("buffer.putInt(remaining"+p+");");
                }else if(parameter.equals(Buffer.class)) {
                    out.println(paramName+"==null?"+0+":"+paramName+".remaining();");
                    out.println("buffer.putInt(remaining"+p+");");
                }else if(isStructAccessor(parameter)) {
                    out.println(paramName+"==null?0"+":"+paramName+".getBuffer().capacity();");
                    out.println("buffer.putInt(remaining"+p+");");
                }else{
                    out.println("0;    // ignore "+paramName);
                }
            }
        }
    }


}
