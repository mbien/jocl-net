/*
 * Created on Friday, May 27 2011 20:31
 */
package com.mbien.generator;

import com.mbien.opencl.net.remote.CLRemotePlatform;
import com.mbien.opencl.net.remote.CLRemotePlatform.RemoteContextBinding;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.reflect.Modifier.*;

/**
 *
 * @author Michael Bien
 */
public class NetworkBindingGenerator {

    protected final String base;
    protected final String pkage;
    protected final String name;

    protected NetworkBindingGenerator(String base, String pkage, String name) {
        this.base = base;
        this.pkage = pkage;
        this.name = name;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, NoSuchMethodException {

        Class<RemoteContextBinding> targetInterface = CLRemotePlatform.RemoteContextBinding.class;
        String base = "/home/mbien/NetBeansProjects/JOGAMP/jocl-net/gensrc/";

        String pkage = "com/mbien/opencl/net/remote";
        String name = "CLAbstractRemoteContextBinding";

        ClientBindingGenerator clientGen = new ClientBindingGenerator(base, pkage, name);
        clientGen.generate(targetInterface);

        
        name = "CLContextHandler";
        pkage = "com/mbien/opencl/net";

        ServerBindingGenerator serverGen = new ServerBindingGenerator(base, pkage, name);
        serverGen.generate(targetInterface);
        
    }

    protected void createClassHeader(IndentingWriter out, String pkage, List<?> imports, int modifier, String name, Class<?> extended, Class<?>... implemented) {
        
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

    protected void createMethodDeclaration(IndentingWriter out, int modifiers, boolean override, Method method, String... names) {
        createMethodDeclaration(out, modifiers, override, method, names, (Class<? extends Throwable>[])null);
    }

    protected void createMethodDeclaration(IndentingWriter out, int modifiers, boolean override, Method method, String[] names, Class<? extends Throwable>... throwables) {

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

    protected IndentingWriter newWriter() throws IOException {

        File file = new File(base+pkage+"/"+name+".java");
        file.getParentFile().mkdirs();
//        file.createNewFile();

        return new IndentingWriter(new PrintWriter(file));
    }

}
