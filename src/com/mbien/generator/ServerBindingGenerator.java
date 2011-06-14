/*
 * Created on Wednesday, June 01 2011 02:39
 */
package com.mbien.generator;


import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.CLHandler;
import com.mbien.opencl.net.annotation.Unsupported;
import com.mbien.opencl.net.annotation.Unsupported.Kind;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ByteChannel;
import java.util.Arrays;
import java.util.List;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.*;

/**
 * Generates the server side of the binding.
 * @author Michael Bien
 */
public class ServerBindingGenerator extends NetworkBindingGenerator {

    private final Class<?> implInterface;

    ServerBindingGenerator(String base, String pkage, String name, Class<?> implInterface) {
        super(base, pkage, name);
        this.implInterface = implInterface;
    }

    @Override
    void generateBindingFor(Class<?> targetInterface) throws IOException {
        List<Method> methods = sortMethods(targetInterface.getMethods());
        IndentingWriter out = newWriter();
        try{
            createServerLayer(out, name, pkage, implInterface, methods);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }finally{
            out.close();
        }
    }


    private void createServerLayer(IndentingWriter out, String name, String pkage, Class impl, List<Method> methods) throws SecurityException, NoSuchMethodException {

        List<?> importList = asList(
                impl,
                IOException.class,
                "java.nio.*",
                "java.nio.channels.*",
                "com.jogamp.common.nio.*",
                "static "+NativeSizeBuffer.class.getCanonicalName()+".*",
                "static "+Buffers.class.getCanonicalName()+".*",
                "static com.mbien.opencl.net.util.NetBuffers.*"
                );

        
        createClassHeader(out, pkage, importList, PUBLIC, name, CLHandler.class);

        out.indent();

        out.println();
        out.println("private final "+impl.getSimpleName()+" impl;");
        out.println();
        out.println("public "+name+"("+impl.getSimpleName()+" impl) {");
        out.println("    this.impl = impl;");
        out.println("}");

        Method method = CLHandler.class.getDeclaredMethod("handle", ByteChannel.class, Integer.TYPE);
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

        String delegate = "impl";

        out.println("switch(methodID) {");
        out.indent();
        out.println();

        for (int m = 0; m < methods.size(); m++) {
            Method method = methods.get(m);
            createServerSwitchCase(out, m, method, delegate);
        }
        out.println("default: {");
        out.println("    "+exception(UnsupportedOperationException.class, "\"unknown destination: \"+methodID"));
        out.println("}");
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

        if(method.isAnnotationPresent(Unsupported.class)) {

            Unsupported annotation = method.getAnnotation(Unsupported.class);
            Kind value = annotation.value();
            if(value.equals(Kind.NOOP)) {
                out.println("// NOOP");
            }else if(value.equals(Kind.UOE)) {
                out.println(exception(UnsupportedOperationException.class, "\"not supported in binding.\""));
            }else{
                throw new RuntimeException("unexpected annotation value "+annotation);
            }

        }else{
            
            for (int p = 0; p < parameters.length; p++) {

                Class parameter = parameters[p];

                String type = getTypeName(parameter);
                if(Buffer.class.isAssignableFrom(parameter)) {
                    type = ByteBuffer.class.getSimpleName();
                }

                createReadParameterSection(out, parameter, type, p, parameterAnnotations);

                out.println();
            }

            // method call
            Class<?> returnType = method.getReturnType();

            if(!returnType.equals(void.class)) {
                out.print(returnType.getSimpleName()+" ret = ");
            }
            out.print(delegate+"."+method.getName()+"(");

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
                boolean outbound = isOutbound(p, parameterAnnotations);

                if(outbound) {
                    String sizeParam = "size"+p;
                    out.print("if("+sizeParam+" > 0) {");
                    if(parameter.equals(NativeSizeBuffer.class) || isStructAccessor(parameter)) {
                        out.print(" channel.write(p"+p+".getBuffer()); ");
                    }else if(Buffer.class.isAssignableFrom(parameter)) {
                        out.print(" channel.write(p"+p+"); ");
                    }else if(parameter.isArray()) {
                        out.print("writeArray(channel, buffer, p"+p+");");
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
                }else if(returnType.getCanonicalName().equals("void")) {
                    // nothing to do here
                }else{
                    throw new RuntimeException("unexpected type: "+returnType);
                }
            }
            out.println();

            out.println("break;");
        }

        out.unindent();
        out.println();
        out.println("}");
    }
    
    private void createReadParameterSection(IndentingWriter out, Class parameter, String type, int p, Annotation[][] parameterAnnotations) throws RuntimeException {

        boolean in = isInbound(p, parameterAnnotations);
        boolean unsupported = isAnnotatedWith(p, parameterAnnotations, Unsupported.class);

        String parName = "p"+p;

        if(unsupported) {
            out.print(type +" "+parName+" = "+nullOf(parameter)+";");
        }else if(parameter.isPrimitive()) {
            out.print(type +" "+parName+" = ");
            createReadPrimitiveSection(out, parameter);
        }else if(parameter.isArray()) {
            Class component = parameter.getComponentType();

            String lenght = "size"+p;
            out.println(type +" "+parName+" = null;");
            out.println("int "+lenght+" = readInt(channel, buffer);");
            out.println("if("+lenght+" > 0) {");
            out.println("    "+parName+" = new "+getTypeName(component)+"["+lenght+"];");
            if(in) {
                out.println("    for(int i = 0; i < "+lenght+"; i++) {");
                out.print("        "+parName+"[i] = ");
                if(component.equals(String.class)) {
                    out.println("readString(channel, readInt(channel, buffer));");
                }else if(component.isPrimitive()) {
                    createReadPrimitiveSection(out, component);
                }
                out.println("    }");
            }
            
            out.println("}");
        }else{
            out.println(type +" "+parName+" = null;");
            String sizeParam = "size"+p;

            out.print("int "+sizeParam+" = ");

            // check buffer size
            if(Buffer.class.isAssignableFrom(parameter) || parameter.equals(NativeSizeBuffer.class) || isStructAccessor(parameter) || parameter.equals(String.class)) {

                out.println("readInt(channel, buffer);");
                out.println("if("+sizeParam+" > 0) {");
                out.print("    "+parName+" = ");
                if(parameter.equals(NativeSizeBuffer.class)) {
                    out.println("allocateDirect("+sizeParam+"/elementSize());");
                    if(in) {
                        out.println("    readBuffer(channel, "+parName+");");
                    }
                }else if(Buffer.class.isAssignableFrom(parameter)) {
                    out.println("newDirectByteBuffer("+sizeParam+");");
                    if(in) {
                        out.println("    readBuffer(channel, "+parName+");");
                    }
                }else if(isStructAccessor(parameter)) {
                    out.println(getTypeName(parameter)+".create(newDirectByteBuffer("+sizeParam+"));");
                    if(in) {
                        out.println("    readBuffer(channel, "+parName+".getBuffer());");
                    }
                }else if(parameter.equals(String.class)) {
                    out.println("readString(channel, "+sizeParam+");");
                }else{
                    out.println("null;");
                }
                out.println("}");
            }else{
                throw new RuntimeException("unknown type "+parameter);
            }

        }
    }

    private void createReadPrimitiveSection(IndentingWriter out, Class parameter) {
        if(parameter.equals(long.class)) {
            out.println("readLong(channel, buffer);");
        }else if(parameter.equals(int.class)) {
            out.println("readInt(channel, buffer);");
        }else if(parameter.equals(double.class)) {
            out.println("readDouble(channel, buffer);");
        }else if(parameter.equals(float.class)) {
            out.println("readFloat(channel, buffer);");
        }else{
            throw new RuntimeException();
        }
    }

}
