/*
 * Created on Saturday, June 04 2011 00:20
 */
package com.mbien.generator;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.CLHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.reflect.Modifier.*;
import static junit.framework.Assert.*;
import static java.lang.System.*;
import static java.util.Arrays.*;
import static com.jogamp.common.nio.Buffers.*;
import static com.jogamp.common.nio.NativeSizeBuffer.*;

/**
 *
 * @author Michael Bien
 */
public class GeneratorTest {

    private static byte id;

    private static String project;
    private static String gensrc;
    private static String dest;
    private static String clientPackage;
    private static String serverPackage;
    private static String clientImplName;

    @BeforeClass
    public static void generatorTest() throws Exception {

        project = "/home/mbien/NetBeansProjects/JOGAMP/jocl-net/";

        gensrc = project+"build/gensrc/";
        dest = project+"build/genbin/";
        clientPackage = "com/mbien/test/net/client";
        serverPackage = "com/mbien/test/net/server";
        id = (byte)1;
        clientImplName = "RemoteFooBarBinding";

        NetworkBindingGenerator.generateBinding(id, "FooBar", Target.class, gensrc, clientPackage, serverPackage);

        compile(new File[] {
            new File(project+"test/"+Target.class.getCanonicalName().replace('.', '/')+".java"),
            new File(gensrc+clientPackage+"/CLAbstract"+clientImplName+".java"),
            new File(gensrc+serverPackage+"/CLFooBarHandler.java"),
        }, dest);


        // generate simple client impl
        NetworkBindingGenerator gen = new NetworkBindingGenerator(gensrc, clientPackage, clientImplName) {

            @Override
            void generateBindingFor(Class<?> targetInterface) throws IOException {
                IndentingWriter w = newWriter();
                Class<?> generated;
                try {
                    String className = pkage.replace('/', '.')+".CLAbstract"+clientImplName;
                    generated = Class.forName(className);
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                createClassHeader(w, pkage, new ArrayList(), PUBLIC, name, generated);

                w.println();
                w.println("private final java.nio.channels.ByteChannel channel;");
                w.println("private final java.nio.ByteBuffer buffer;");
                w.println("public "+name+"(java.nio.channels.ByteChannel channel, java.nio.ByteBuffer buffer) {");
                w.println("    this.buffer = buffer;");
                w.println("    this.channel = channel;");
                w.println("}");
                w.println("public java.nio.ByteBuffer getBuffer() {return buffer;}");
                w.println("public java.nio.channels.ByteChannel getChannel() {return channel;}");
                w.println();

                w.println("}");
                w.close();
            }
        };
        gen.generateBindingFor(null);

        compile(new File[]{new File(gensrc+clientPackage+"/"+clientImplName+".java")}, dest);

    }

    @Test
    public void clientTest() throws Exception {

        out.println("generator-client-test");
        out.println("native size_t is: "+elementSize());

        Constructor<?> clientConstructor = Class.forName(clientPackage.replace('/', '.')+"."+clientImplName)
                                                .getConstructor(ByteChannel.class, ByteBuffer.class);

        Random rnd = new Random();
        final long             p0 = rnd.nextLong();
        final int              p1 = rnd.nextInt();
        final NativeSizeBuffer p2 = allocateDirect(new long[]{rnd.nextLong(),rnd.nextLong(),rnd.nextLong()});
        final IntBuffer        p3 = newDirectIntBuffer(new int[]{rnd.nextInt(),rnd.nextInt(),rnd.nextInt()});
        final IntBuffer        p4 = newDirectIntBuffer(4);
        final int          expRet = rnd.nextInt();

        ByteBuffer writeBuffer = newDirectByteBuffer(SIZEOF_BYTE+SIZEOF_INT
                                                    +SIZEOF_LONG+SIZEOF_INT
                                                    +SIZEOF_INT+p2.getBuffer().capacity()
                                                    +SIZEOF_INT+p3.capacity()*SIZEOF_INT
                                                    +SIZEOF_INT);//intOut size field
        ByteBuffer readBuffer = newDirectByteBuffer(p4.capacity()*SIZEOF_INT+SIZEOF_LONG);

        readBuffer.putInt(rnd.nextInt());  //intOut buffer
        readBuffer.putInt(rnd.nextInt());
        readBuffer.putInt(rnd.nextInt());
        readBuffer.putInt(rnd.nextInt());

        readBuffer.putLong(expRet); //return
        readBuffer.rewind();

        DebugChannel channel = new DebugChannel(writeBuffer, readBuffer);

        Target client = (Target)clientConstructor.newInstance(channel, newDirectByteBuffer(writeBuffer.capacity()));

        long ret = client.test1(p0, p1, p2, p3, p4);
        assertEquals(expRet, ret);
        assertEquals(0, p4.position());

        writeBuffer.rewind();

        assertEquals(id, writeBuffer.get());
        assertEquals(0,  writeBuffer.getInt());
        assertEquals(p0, writeBuffer.getLong());
        assertEquals(p1, writeBuffer.getInt());

        if(elementSize() == SIZEOF_LONG) {
            assertEquals(p2.getBuffer().capacity(), writeBuffer.getInt());
            assertEquals(p2.get(), writeBuffer.getLong());
            assertEquals(p2.get(), writeBuffer.getLong());
            assertEquals(p2.get(), writeBuffer.getLong());
        }else{
            assertEquals(p2.getBuffer().capacity(), writeBuffer.getInt());
            assertEquals((int)p2.get(), writeBuffer.getInt());
            assertEquals((int)p2.get(), writeBuffer.getInt());
            assertEquals((int)p2.get(), writeBuffer.getInt());
        }

        assertEquals(p3.capacity()*SIZEOF_INT, writeBuffer.getInt());
        assertEquals(p3.get(), writeBuffer.getInt());
        assertEquals(p3.get(), writeBuffer.getInt());
        assertEquals(p3.get(), writeBuffer.getInt());

        assertEquals(p4.capacity()*SIZEOF_INT+SIZEOF_LONG, readBuffer.position());

        // out buffer
        assertEquals(p4.capacity()*SIZEOF_INT, writeBuffer.getInt());
        readBuffer.rewind();
        for (int i = 0; i < p4.capacity(); i++) {
            assertEquals(p4.get(), readBuffer.getInt());
        }

    }

    @Test
    public void serverTest() throws Exception {
        
        out.println("generator-server-test");

        Constructor<?> serverConstructor = Class.forName(serverPackage.replace('/', '.')+".CLFooBarHandler")
                                                .getConstructor(Target.class);

        Target target = new Target() {
            
            boolean test2Called = false;
            
            @Override
            public long test1(long longVal, int intVal, NativeSizeBuffer nativeSize, IntBuffer intIn, IntBuffer intOut) {
                return 1;
            }
            
            @Override
            public void test2() {
                if(test2Called) {
                    fail("already called");
                }
                test2Called = true;
            }
            
//            @Override
//            public long test3(int a, String foo, int b) {
//                return 2;
//            }

        };

        CLHandler server = (CLHandler) serverConstructor.newInstance(target);

        //todo

    }


    private static void compile(File[] files, String destination) throws IOException {

        out.println("compiling files:\n    " + asList(files));
        new File(destination).mkdirs();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, null, null);

        Iterable<? extends JavaFileObject> fileObj = fileManager.getJavaFileObjects(files);

        boolean success = compiler.getTask( new OutputStreamWriter(out),
                                            fileManager,
                                            collector,
                                            asList("-d", destination/*, "-verbose"*/),
                                            null,
                                            fileObj ).call();

        fileManager.close();

        List<Diagnostic<? extends JavaFileObject>> list = collector.getDiagnostics();
        if(!list.isEmpty() || !success) {
            for (Diagnostic<? extends JavaFileObject> d : list) {
                out.println("Error in "+d.getSource().getName()+" line "+ d.getLineNumber());
                out.println(d.getMessage(Locale.ENGLISH));
            }
            fail("compilation failed");
        }

    }

    private static class DebugChannel implements ByteChannel {

        private boolean open = true;

        private final ByteBuffer wb;
        private final ByteBuffer rb;

        public DebugChannel(ByteBuffer wb, ByteBuffer rb) {
            this.wb = wb;
            this.rb = rb;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int remaining = dst.remaining();
            if(rb.remaining() < remaining) {
                throw new RuntimeException("readbuffer to small, requested "+remaining+"bytes");
            }
            rb.limit(rb.position()+remaining);
            dst.put(rb);
            rb.limit(rb.capacity());
            return remaining;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws IOException {
            open = false;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int remaining = src.remaining();
            wb.put(src);
            return remaining;
        }
    }


}
