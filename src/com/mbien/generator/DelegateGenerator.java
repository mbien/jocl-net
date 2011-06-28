/*
 * Created on Tuesday, June 21 2011 02:32
 */
package com.mbien.generator;

import com.jogamp.common.nio.NativeSizeBuffer;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.*;

/**
 *
 * @author Michael Bien
 */
public class DelegateGenerator extends NetworkBindingGenerator {

    private final String delegate = "delegate";

    DelegateGenerator(String base, String pkage, String name) {
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
                "java.nio.*",
                Queue.class,
                ConcurrentLinkedQueue.class,
                "com.jogamp.common.nio.*",
                "static "+NativeSizeBuffer.class.getCanonicalName()+".*");

        createClassHeader(out, pakage, importList, PUBLIC, name, null, targetInterface);

        out.indent();
        out.println("public static class Event{ public final long queue; public final long event; public Event(long c, long e) {this.event = e; this.queue = c;}}");
        out.println("private final "+getTypeName(targetInterface)+" "+delegate+";");
        out.println("private final "+getTypeName(NativeSizeBuffer.class)+" buffer = allocateDirect(1);");
        out.println("public final "+getTypeName(Queue.class)+"<Event> events = new "+getTypeName(ConcurrentLinkedQueue.class)+"<Event>();");
        out.println();
        out.println("public "+name+"("+getTypeName(targetInterface) +" "+delegate+") {");
        out.println("    this."+delegate+" = "+delegate+";");
        out.println("}");

        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            delegatingMethodsImplementation(out, i, method);
        }
        out.unindent();

        out.println();
        out.println("}");
    }


    private void delegatingMethodsImplementation(IndentingWriter out, int id, Method method) {

        createMethodDeclaration(out, PUBLIC, true, method);
        out.indent();

        out.println();

        Class<?>[] parameters = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();

        boolean events = false;
        if(parameters.length > 2 && parameters[parameters.length-2].equals(NativeSizeBuffer.class)
                                 && parameters[parameters.length-1].equals(NativeSizeBuffer.class)) {
            out.println("if(p"+(parameters.length-1)+" == null) {");
            out.println("    p"+(parameters.length-1)+" = buffer;");
            out.println("}");
            events = true;
        }

        if(!returnType.equals(void.class)) {
            out.print(getTypeName(returnType)+" ret = ");
        }

        out.print(delegate+"."+method.getName()+"(");

        for (int p = 0; p < parameters.length; p++) {
            out.print("p"+p);
            if(p < parameters.length-1) {
                out.print(", ");
            }
        }
        out.println(");");

        if(events) {
            out.println("events.add(new Event(p0, p"+(parameters.length-1)+".get(0)));");
        }


        if(!returnType.equals(void.class)) {
            out.print("return ret;");
        }

        out.unindent();
        out.println();
        out.println("}");
    }


}
