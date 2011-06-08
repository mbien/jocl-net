/*
 * Created on Friday, May 27 2011 20:31
 */
package com.mbien.generator;

import com.jogamp.common.nio.NativeBuffer;
import com.mbien.generator.interfaces.RemoteContextBinding;
import com.mbien.generator.interfaces.RemoteProgramBinding;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.reflect.Modifier.*;

/**
 *
 * @author Michael Bien
 */
public abstract class NetworkBindingGenerator {

    protected final String base;
    protected final String pkage;
    protected final String name;
    protected final Set<String> imports;

    protected NetworkBindingGenerator(String base, String pkage, String name) {
        this.base = base;
        this.pkage = pkage;
        this.name = name;
        this.imports = new HashSet<String>();
    }

    public static void main(String[] args) throws IOException {

        String base = "/home/mbien/NetBeansProjects/JOGAMP/jocl-net/gensrc/";
        String clientPackage = "com/mbien/opencl/net/remote";
        String serverPackage = "com/mbien/opencl/net";

        generateBinding((byte)3, "Context", RemoteContextBinding.class, base, clientPackage, serverPackage);
        generateBinding((byte)4, "Program", RemoteProgramBinding.class, base, clientPackage, serverPackage);
        
    }

    public static void generateBinding(byte id, String name, Class<?> targetInterface, String base, String clientPackage, String serverPackage) throws IOException {
        System.out.println("generating "+name+" binding");

        ClientBindingGenerator clientGen = new ClientBindingGenerator(base, clientPackage, "CLAbstractRemote"+name+"Binding", id);
        clientGen.generateBindingFor(targetInterface);

        ServerBindingGenerator serverGen = new ServerBindingGenerator(base, serverPackage, "CL"+name+"Handler");
        serverGen.generateBindingFor(targetInterface);
    }

    abstract void generateBindingFor(Class<?> targetInterface) throws IOException;
    

    /**
     * Complete class declaration header, including package, imports and even the opening bracket.
     */
    protected void createClassHeader(IndentingWriter out, String pkage, List<?> imports, int modifier, String name, Class<?> extended, Class<?>... implemented) {

        this.imports.clear();

        out.println("/* generated, do not edit ["+new Date()+"] */");
        String packageString = pkage.replace('/', '.');
        this.imports.add(packageString+".*");

        out.println("package "+packageString+";");
        out.println();
        for (Object object : imports) {
            String importString;
            if(object instanceof Class) {
                importString = ((Class)object).getCanonicalName();
            }else{
                importString = object.toString();
            }
            out.println("import "+importString+";");

            // add import to list for later type resolution
            if(!importString.startsWith("static ")) {
                this.imports.add(importString);
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
            out.print(" extends "+getTypeName(extended));
        }
        if(implemented.length > 0) {
            out.print(" implements");
            for (int i = 0; i < implemented.length; i++) {
                Class<?> inter = implemented[i];
                out.print(" "+getTypeName(inter));
                if(i < implemented.length-1) {
                    out.print(',');
                }
            }
        }
        out.println(" {");
        
        out.println();
    }

    protected void createMethodDeclaration(IndentingWriter out, int modifiers, boolean override, Method method, String... names) {
        createMethodDeclaration(out, modifiers, override, method, names, (Class<? extends Throwable>[])null);
    }

    protected void createMethodDeclaration(IndentingWriter out, int modifiers, boolean override, Method method, String[] names, Class<? extends Throwable>... throwables) {

        out.println();
        if(override) {
            out.println("@Override");
        }
        createDeclarationModifiers(out, modifiers);
        
        out.print(" "+getTypeName(method.getReturnType())+" "+method.getName()+"(");

        Class<?>[] parameters = method.getParameterTypes();
        for (int p = 0; p < parameters.length; p++) {
            Class<?> param = parameters[p];
            out.print(getTypeName(param)+" ");
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
                out.print(" "+getTypeName(t));
                if(i < throwables.length-1) {
                    out.print(',');
                }
            }
        }
        out.println(" {");
    }

    protected void createDeclarationModifiers(IndentingWriter out, int modifiers) {
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

    protected List<Method> sortMethods(Method[] methods) {

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

    protected boolean isAnnotatedWith(int p, Annotation[][] parameterAnnotations, Class annotation) {
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

    protected String getTypeName(Class<?> type) {

        // direct match
        if(imports.contains(type.getCanonicalName())) {
            return type.getSimpleName();
        }

        Package p = type.getPackage();

        if(p != null) {
            String packageName = p.getName();

            // default imports
            if(packageName.equals("java.lang")) {
                return type.getSimpleName();
            }

            // custom imports
            for (String imp : imports) {
                if(imp.endsWith(".*")) {
                    if(packageName.equals(imp.substring(0, imp.length()-2))) {
                        return type.getSimpleName();
                    }
                }
            }
        }

        return type.getCanonicalName();
    }

    protected IndentingWriter newWriter() throws IOException {

        File file = new File(base+pkage+"/"+name+".java");
        file.getParentFile().mkdirs();
//        file.createNewFile();

        return new IndentingWriter(new PrintWriter(file));
    }

    /*
     * TODO this is a big hack but we will have to refactor gluegen to fix this
     */
    protected boolean isStructAccessor(Class<?> parameter) {
        if(NativeBuffer.class.isAssignableFrom(parameter)) {
            return false;
        }
        for (Method method : parameter.getMethods()) {
            Class<?> ret = method.getReturnType();
            if (method.getName().endsWith("getBuffer") && ret != null && ret.equals(ByteBuffer.class)) {
                return true;
            }
        }
        return false;
    }

}
