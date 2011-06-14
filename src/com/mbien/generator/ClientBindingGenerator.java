/*
 * Created on Wednesday, June 01 2011 02:42
 */
package com.mbien.generator;

import com.jogamp.common.nio.NativeBuffer;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.annotation.Unsupported;
import com.mbien.opencl.net.annotation.Unsupported.Kind;
import com.mbien.opencl.net.remote.CLRemoteBinding;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.*;

/**
 * Generates the client side of the binding.
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
                "static com.mbien.opencl.net.util.NetBuffers.*");

        createClassHeader(out, pakage, importList, PUBLIC, name, CLRemoteBinding.class, targetInterface);

        out.indent();
        out.println("public final static byte AID = "+BINDING_ID+";");
        out.println();
        out.println("public "+name+"(com.mbien.opencl.net.remote.RemoteNode node) {");
        out.println("    super(node);");
        out.println("}");
        out.unindent();

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

        if(method.isAnnotationPresent(Unsupported.class)) {
            Kind value = method.getAnnotation(Unsupported.class).value();
            if(value.equals(Kind.NOOP)) {
                out.println("// NOOP");
            }else if(value.equals(Kind.UOE)) {
                out.println(exception(UnsupportedOperationException.class, "\"method not supported\""));
            }

            out.unindent();
            out.println();
            out.println("}");
            out.unindent();
            return;
        }

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
            createWriteParameterSection(out, "p"+p, parameter, p, parameterAnnotations);

        }

        out.println();
        out.println("buffer.flip();");
        out.println("channel.write(buffer); // remote method call");

        out.println();
        out.println("buffer.rewind();");

        // read with @Out annotated parameters
        boolean read = false;
        for (int p = 0; p < parameterTypes.length; p++) {
            Class<?> parameter = parameterTypes[p];
            boolean outbound = isOutbound(p, parameterAnnotations);
            if(outbound) {
                read = true;

                String paramName = "p"+p;

                if(parameter.isArray()) {
                    out.println("if(p"+p+" != null) {");
                    out.println("    readArray(channel, "+paramName+", buffer);");
                    out.println("}");
                }else{
                    String remaining = "remaining"+p;
                    out.println("if("+remaining+" > 0) {");
                    
                    if(parameter.equals(ByteBuffer.class) || NativeBuffer.class.isAssignableFrom(parameter)) {
                        out.println("    readBuffer(channel, "+paramName+");");
                        out.println("    "+paramName+".position("+paramName+".capacity()-"+remaining+");");
                    }else if(Buffer.class.isAssignableFrom(parameter)) {
                        out.println("    readBuffer(channel, "+paramName+", buffer);");
                        out.println("    "+paramName+".position("+paramName+".capacity()-"+remaining+");");
                    }else if(isStructAccessor(parameter)) {
                        out.println("    channel.read("+paramName+".getBuffer());");
                        out.println("    "+paramName+".getBuffer().position("+paramName+".getBuffer().capacity()-"+remaining+");");
                    }else{
                        out.println("  /* ignore */ ");
                    }

                    out.println("}");
                }

            }
        }
        if(read) {
            out.println("buffer.rewind();");
        }

        // read and return call result value
        Class<?> returnType = method.getReturnType();
        if(returnType.isPrimitive()) {
            if(returnType.equals(long.class)) {
                out.println("return readLong(channel, buffer);");
            }else if(returnType.equals(int.class)) {
                out.println("return readInt(channel, buffer);");
            }else if(returnType.equals(byte.class)) {
                out.println("return readByte(channel, buffer);");
            }else if(returnType.equals(double.class)) {
                out.println("return readDouble(channel, buffer);");
            }else if(returnType.equals(float.class)) {
                out.println("return readFloat(channel, buffer);");
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
        out.println("    try{ if(channel != null) channel.close(); }catch(IOException ignore) { }");
        out.println("    throw new RuntimeException(ex);");
        out.println("}finally{");
        out.println("    buffer.rewind();");
        out.println("}");
        out.unindent();

        out.println();
        out.unindent();
        out.println("}");
    }

    private void createWriteParameterSection(IndentingWriter out, String paramName, Class<?> parameter, int p, Annotation[][] annotations) throws RuntimeException {

        boolean inbound = isInbound(p, annotations);
        boolean outbound = isOutbound(p, annotations);
        boolean unsupported = isAnnotatedWith(p, annotations, Unsupported.class);

        if(unsupported) {
            out.println("// @Unsupported "+paramName);
        }else if(parameter.isPrimitive()) {
            if(parameter.equals(long.class)) {
                out.println("buffer.putLong("+paramName+");");
            }else if(parameter.equals(byte.class)) {
                out.println("buffer.put("+paramName+");");
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

            out.println("if("+paramName+"==null) {");
            out.println("    buffer.putInt(-1);");
            out.println("}else{");
            out.println("    buffer.putInt("+paramName+".length);");
            if(inbound) {
                out.indent();
                out.println("for(int i = 0; i < "+paramName+".length; i++) {");
                out.indent();
                createWriteParameterSection(out, paramName+"[i]", component, p, annotations);
                out.unindent();
                out.println();
                out.unindent();
                out.println("}");
            }
            out.println("}");

        }else{

            if(outbound) {
                out.print("int remaining"+p+" = ");

                if(Buffer.class.isAssignableFrom(parameter) || parameter.equals(NativeSizeBuffer.class)) {
                    out.println(paramName+"==null?0:"+paramName+".remaining();");
                }else if(isStructAccessor(parameter)){
                    out.println(paramName+"==null?0:"+paramName+".getBuffer().remaining();");
                }
                
                if(!inbound) {
                    if(parameter.equals(IntBuffer.class) | parameter.equals(FloatBuffer.class)) {
                        out.println("buffer.putInt(remaining"+p+"*4);");
                    }else if(parameter.equals(LongBuffer.class) | parameter.equals(DoubleBuffer.class)){
                        out.println("buffer.putInt(remaining"+p+"*8);");
                    }else if(parameter.equals(NativeSizeBuffer.class) ){
                        out.println("buffer.putInt(remaining"+p+"*elementSize());");
                    }else if(isStructAccessor(parameter) || parameter.equals(ByteBuffer.class)) {
                        out.println("buffer.putInt(remaining"+p+");");
                    }else if(parameter.equals(Buffer.class)) {
                        out.println("buffer.putInt(remaining"+p+"*Buffers.sizeOfBufferElem("+paramName+"));");
                    }
                }
            }

            if(inbound) {
                if(NativeBuffer.class.isAssignableFrom(parameter) || Buffer.class.isAssignableFrom(parameter)){
                    out.println("putBuffer(buffer, "+paramName+");");
                }else if(parameter.equals(String.class)) {
                    out.println("putString(buffer, "+paramName+");");
                }else{
                    out.println("// ignore "+paramName);
                }

            }
        }
    }


}
