/*
 * Created on Wednesday, June 01 2011 02:39
 */
package com.mbien.generator;


import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.CLHandler;
import com.mbien.opencl.net.annotation.Out;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ByteChannel;
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

            Class<?>[] interfaces = targetInterface.getInterfaces();
            for (Class<?> interf : interfaces) {
                if(interf.getMethods().length == methods.size()) {

                    List<Method> methods2 = sortMethods(interf.getMethods());
                    boolean matching = true;
                    for (int i = 0; i < methods.size(); i++) {
                        Method method1 = methods.get(i);
                        Method method2 = methods2.get(i);
                        if(!method1.getName().equals(method2.getName())) {
                            matching = false;
                            break;
                        }
                    }
                    if(matching) {
                        targetInterface = interf;
                        break;
                    }
                }
            }

            createServerLayer(out, name, pkage, targetInterface, methods);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        }finally{
            out.close();
        }
    }


    private void createServerLayer(IndentingWriter out, String name, String pkage, Class target, List<Method> methods) throws SecurityException, NoSuchMethodException {

        List<?> importList = asList(
                target,
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
        out.println("private final "+target.getSimpleName()+" impl;");
        out.println();
        out.println("public "+name+"("+target.getSimpleName()+" impl) {");
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

            createReadParameterSection(out, parameter, type, p, in);

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
            boolean _out = isAnnotatedWith(p, parameterAnnotations, Out.class);

            if(_out && parameter.isArray()) {
                out.println("// ignoring array p"+p);
                continue;
            }

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
            }else if(returnType.getCanonicalName().equals("void")) {
                // nothing to do here
            }else{
                throw new RuntimeException("unexpected type: "+returnType);
            }
        }
        out.println();

        out.println("break;");

        out.unindent();
        out.println();
        out.println("}");
    }
    
    private void createReadParameterSection(IndentingWriter out, Class parameter, String type, int p, boolean in) throws RuntimeException {

        String parName = "p"+p;

        if(parameter.isPrimitive()) {

            out.print(type +" "+parName+" = ");

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
        }else if(parameter.isArray()) {
            Class component = parameter.getComponentType();

            String lenght = parName+"lenght";
            out.println(type +" "+parName+" = null;");
            out.println("int "+lenght+" = readInt(channel, buffer);");
            out.println("if("+lenght+" > 0) {");

            if(component.equals(String.class)) {
                out.println("    "+parName+" = new "+getTypeName(component)+"["+lenght+"];");
                out.println("    for(int i = 0; i < "+lenght+"; i++) {");
                out.println("        "+parName+"[i] = readString(channel, readInt(channel, buffer));");
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
                if(parameter.equals(java.nio.IntBuffer.class)) {
                    out.println("newDirectByteBuffer("+sizeParam+");");
                    if(in) {
                        out.println("    readInts(channel, "+parName+");");
                    }
                }else if(parameter.equals(NativeSizeBuffer.class)) {
                    out.println("allocateDirect("+sizeParam+"/elementSize());");
                    if(in) {
                        out.println("    readBytes(channel, "+parName+");");
                    }
                }else if(parameter.equals(ByteBuffer.class)) {
                    out.println("newDirectByteBuffer("+sizeParam+");");
                    if(in) {
                        out.println("    readBytes(channel, "+parName+");");
                    }
                }else if(parameter.equals(Buffer.class)) {
                    out.println("newDirectByteBuffer("+sizeParam+");");
                    if(in) {
                        out.println("    readBytes(channel, (ByteBuffer)"+parName+");");
                    }
                }else if(isStructAccessor(parameter)) {
                    out.println(getTypeName(parameter)+".create(newDirectByteBuffer("+sizeParam+"));");
                    if(in) {
                        out.println("    readBytes(channel, "+parName+".getBuffer());");
                    }
                }else if(parameter.equals(String.class)) {
                    out.println("readString(channel, "+sizeParam+");");
                }else{
                    out.println("null;");
                }
                out.println("}");
            }else{
                out.println("0; // unknown");
            }

        }
    }

}
