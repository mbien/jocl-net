/*
 * Created on Friday, May 27 2011 20:31
 */
package com.mbien.generator;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.CLHandler;
import com.mbien.opencl.net.remote.CLRemoteBinding;
import com.mbien.opencl.net.remote.CLRemotePlatform;
import com.mbien.opencl.net.remote.CLRemotePlatform.RemoteContextBinding;
import com.mbien.opencl.net.annotation.Out;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.*;

/**
 *
 * @author Michael Bien
 */
public class NetworkBindingGenerator {

    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchMethodException {

        Class<RemoteContextBinding> targetInterface = CLRemotePlatform.RemoteContextBinding.class;
        String base = "/home/mbien/NetBeansProjects/JOGAMP/jocl-net/gensrc/";

        List<Method> methods = sortMethods(targetInterface.getMethods());

        String pkage = "com/mbien/opencl/net/remote";
        String name = "CLAbstractRemoteContextBinding";
        
        IndentingWriter out = newWriter(base, pkage, name);
        try{
            createClientLayer(out, pkage, name, targetInterface, methods);
        }finally{
            out.close();
        }
        
        
        name = "CLContextHandler";
        pkage = "com/mbien/opencl/net";
        
        out = newWriter(base, pkage, name);
        try{
            createServerLayer(out, name, pkage, methods);
        }finally{
            out.close();
        }

    }

    private static void createClientLayer(IndentingWriter out, String pakage, String name, Class<RemoteContextBinding> targetInterface, List<Method> methods) {
        
        List<?> imports = asList(
                IOException.class,
                "java.nio.*",
                "java.nio.channels.*",
                "com.jogamp.common.nio.*",
                "static com.jogamp.common.nio.NativeSizeBuffer.*",
                "static com.jogamp.common.nio.Buffers.*");

        createClassHeader(out, pakage, imports, PUBLIC|ABSTRACT, name, null, targetInterface, CLRemoteBinding.class);

        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            createClientMethodImplementation(out, i, method);
        }

        out.println();
        out.println("}");
    }


    private static void createClientMethodImplementation(IndentingWriter out, int id, Method method) {

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
                    if(parameter.equals(java.nio.IntBuffer.class)) {
                        out.println("    putInts(buffer, p"+p+");");
                    }else if(parameter.equals(NativeSizeBuffer.class)) {
                        out.println("    putBytes(buffer, p"+p+".getBuffer());");
                    }else{
                        out.println("    // ignore p"+p);
                    }
                }else{
                    out.print("    int remaining"+p+" = ");

                    if(parameter.equals(java.nio.IntBuffer.class)) {
                        out.println("p"+p+"==null?"+0+":p"+p+".remaining()*4;");
                        out.println("    buffer.putInt(remaining"+p+");");
                    }else if(parameter.equals(NativeSizeBuffer.class)) {
                        out.println("p"+p+"==null?"+0+":p"+p+".remaining()*elementSize();");
                        out.println("    buffer.putInt(remaining"+p+");");
                    }else if(parameter.equals(java.nio.Buffer.class)) {
                        out.println("p"+p+"==null?"+0+":p"+p+".remaining();");
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

        Class<?> returnType = method.getReturnType();
        out.println();
        out.println("    buffer.rewind();");

        boolean read = false;
        for (int p = 0; p < parameterTypes.length; p++) {
            Class<?> parameter = parameterTypes[p];
            boolean in = !isAnnotatedWith(p, parameterAnnotations, Out.class);
            if(!in) {
                read = true;
                out.println("    if(remaining"+p+" > 0) {");
                if(parameter.equals(java.nio.IntBuffer.class)) {
                    out.println("        readBuffer(channel, p"+p+", buffer);");
                }else if(parameter.equals(NativeSizeBuffer.class)) {
                    out.println("        channel.read(p"+p+".getBuffer());");
                }else if(parameter.equals(java.nio.Buffer.class)) {
                    out.println("        channel.read((ByteBuffer)p"+p+");");
                }
                out.println("        p"+p+".rewind();");
                out.println("    }");
            }
        }
        if(read) {
            out.println("    buffer.rewind();");
        }

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

    private static void createServerLayer(IndentingWriter out, String name, String pkage, List<Method> methods) throws SecurityException, NoSuchMethodException {

        List<?> imports = asList(
                "com.jogamp.opencl.llb.CL",
                IOException.class,
                "java.nio.*",
                "java.nio.channels.*",
                "com.jogamp.common.nio.*",
                "static com.jogamp.common.nio.NativeSizeBuffer.*",
                "static com.jogamp.common.nio.Buffers.*"
                );

        createClassHeader(out, pkage, imports, PUBLIC, name, CLHandler.class);

        //TODO remove
        out.println("    public CLContextHandler(CL cl) {");
        out.println("        super(cl);");
        out.println("    }");

        out.indent();

        Method method = CLHandler.class.getDeclaredMethod("handle", SocketChannel.class, Integer.TYPE);
        createMethodDeclaration(out, PUBLIC, true, method, new String[]{"channel", "methodID"}, IOException.class);

        createServerHandlerImplementation(out, methods);
        out.unindent();

        out.println("}");
        out.println("}");
    }

    private static void createServerHandlerImplementation(IndentingWriter out, List<Method> methods) {

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

        out.unindent();
        out.println();
        out.println("}");

    }

    private static void createServerSwitchCase(IndentingWriter out, int m, Method method, String delegate) throws RuntimeException {

        Class<?>[] parameters = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        out.indent();
        out.println("case "+m+": {"+" // "+method.getName());

        for (int p = 0; p < parameters.length; p++) {

            Class parameter = parameters[p];

            boolean in = !isAnnotatedWith(p, parameterAnnotations, Out.class);

            String type = parameter.getCanonicalName();
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

                // check buffer size
                if(Buffer.class.isAssignableFrom(parameter) || parameter.equals(NativeSizeBuffer.class)) {
                    out.println("int "+sizeParam+" = readInt(channel, buffer);");
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
                            out.println("    readBytes((ByteBuffer)channel, p"+p+");");
                        }
                    }else{
                        out.println("null;");
                    }
                    out.println("}");
                }else{
                    
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

        // write out parameters
        for (int p = 0; p < parameters.length; p++) {
            boolean _out = isAnnotatedWith(p, parameterAnnotations, Out.class);

            if(_out) {
                String sizeParam = "size"+p;
                out.print("if("+sizeParam+" > 0) {");
                if(parameters[p].equals(NativeSizeBuffer.class)) {
                    out.print(" channel.write(p"+p+".getBuffer()); ");
                }else{
                    out.print(" channel.write(p"+p+"); ");
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

    private static void createClassHeader(IndentingWriter out, String pkage, List<?> imports, int modifier, String name, Class<?> extended, Class<?>... implemented) {
        
        out.println("/* generated, do not edit ["+new Date()+"] */");
        out.println("package "+pkage.replace('/', '.')+";");
        out.println();
        for (Object object : imports) {
            if(object instanceof Class) {
                out.println("import "+((Class)object).getCanonicalName()+";");
            }else{
                out.println("import "+object+";");
            }
        }
        out.println();
        
        createDeclarationModifiers(out, modifier);
        
        if(isInterface(modifier)) {
            out.print(" interface "+name);
        }else{
            out.print(" class "+name);
        }
        if(extended != null) {
            out.print(" extends "+extended.getCanonicalName());
        }
        if(implemented.length > 0) {
            out.print(" implements");
            for (int i = 0; i < implemented.length; i++) {
                Class<?> inter = implemented[i];
                out.print(" "+inter.getCanonicalName());
                if(i < implemented.length-1) {
                    out.print(',');
                }
            }
        }
        out.println(" {");
        
        out.println();
    }

    private static void createMethodDeclaration(IndentingWriter out, int modifiers, boolean override, Method method, String... names) {
        createMethodDeclaration(out, modifiers, override, method, names, (Class<? extends Throwable>[])null);
    }

    private static void createMethodDeclaration(IndentingWriter out, int modifiers, boolean override, Method method, String[] names, Class<? extends Throwable>... throwables) {

        out.println();
        if(override) {
            out.println("@Override");
        }
        createDeclarationModifiers(out, modifiers);
        
        out.print(" "+method.getReturnType().getName()+" "+method.getName()+"(");

        Class<?>[] parameters = method.getParameterTypes();
        for (int p = 0; p < parameters.length; p++) {
            Class<?> param = parameters[p];
            out.print(param.getCanonicalName()+" ");
            if(names.length > p) {
                out.print(names[p]);
            }else{
                out.print("p"+p);
            }
            if(p < parameters.length-1) {
                out.print(", ");
            }
        }
        out.print(")");
        if(throwables != null && throwables.length > 0) {
            out.print(" throws");
            for (int i = 0; i < throwables.length; i++) {
                Class<?> t = throwables[i];
                out.print(" "+t.getName());
                if(i < throwables.length-1) {
                    out.print(',');
                }
            }
        }
        out.println(" {");
    }

    private static void createDeclarationModifiers(IndentingWriter out, int modifiers) {
        if(isPublic(modifiers)) {
            out.print("public");
        }else if(isPrivate(modifiers)) {
            out.print("private");
        }else if(isProtected(modifiers)) {
            out.print("protected");
        }
        if(isStatic(modifiers)) {
            out.print(" static");
        }
        if(isAbstract(modifiers)) {
            out.print(" abstract");
        }else if(isFinal(modifiers)) {
            out.print(" final");
        }
    }

    private static List<Method> sortMethods(Method[] methods) {

        Map<String, Method> map = new TreeMap<String, Method>();

        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            String types = "";
            for (Class<?> type : parameterTypes) {
                types+=type.getName()+" ";
            }
            map.put(method.getName()+":"+types, method);
        }

        return new ArrayList<Method>(map.values());
    }

    private static boolean isAnnotatedWith(int p, Annotation[][] parameterAnnotations, Class annotation) {
        if(parameterAnnotations.length >= p && parameterAnnotations[p].length > 0) {
            Annotation[] annotations = parameterAnnotations[p];
            for (Annotation an : annotations) {
                if(an.annotationType().equals(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static IndentingWriter newWriter(String base, String pkage, String name) throws IOException {

        File file = new File(base+pkage+"/"+name+".java");
        file.getParentFile().mkdirs();
//        file.createNewFile();

        return new IndentingWriter(new PrintWriter(file));
    }

}
