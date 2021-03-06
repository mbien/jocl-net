/*
 * Created on Saturday, June 04 2011 00:20
 */
package com.mbien.generator;

import com.mbien.opencl.net.util.DebugChannel;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.CLHandler;
import com.mbien.opencl.net.remote.RemoteNode;
import com.mbien.opencl.net.util.NetBuffers;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ByteChannel;
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

    private static final Random rnd = new Random();

    private static byte id;

    private static String project;
    private static String gensrc;
    private static String dest;
    private static String clientPackage;
    private static String serverPackage;
    private static String clientImplName;
    private static String serverImplName;

    @BeforeClass
    public static void generatorTest() throws Exception {

        project = "/home/mbien/NetBeansProjects/JOGAMP/jocl-net/";

        gensrc = project+"build/gensrc/";
        dest = project+"build/genbin/";
        clientPackage = "com/mbien/test/net/client";
        serverPackage = "com/mbien/test/net/server";
        id = (byte)1;
        clientImplName = "CLRemoteFooBarBinding";
        serverImplName = "CLFooBarHandler";

        NetworkBindingGenerator.generateBinding(id, "FooBar", Target.class, Target.class, gensrc, clientPackage, serverPackage);

        compile(new File[] {
            new File(project+"test/"+Target.class.getCanonicalName().replace('.', '/')+".java"),
            new File(gensrc+clientPackage+"/"+clientImplName+".java"),
            new File(gensrc+serverPackage+"/"+serverImplName+".java"),
        }, dest);

    }

    @Test
    public void clientTest() throws Exception {

        out.println("generator-client-test");
        out.println("native size_t is: "+elementSize());

        final int   SIZEOF_HEADER = SIZEOF_BYTE+SIZEOF_INT;

        final long             p0 = rnd.nextLong();
        final int              p1 = rnd.nextInt();
        final NativeSizeBuffer p2 = allocateDirect(new long[]{rnd.nextLong(),rnd.nextLong(),rnd.nextLong()});
        final IntBuffer        p3 = newDirectIntBuffer(new int[]{rnd.nextInt(),rnd.nextInt(),rnd.nextInt()});
        final IntBuffer        p4 = newDirectIntBuffer(4);
        final int            ret0 = rnd.nextInt();

        ByteBuffer writeBuffer = newDirectByteBuffer(SIZEOF_HEADER
                                                    +SIZEOF_LONG+SIZEOF_INT
                                                    +SIZEOF_INT+p2.getBuffer().capacity()
                                                    +SIZEOF_INT+p3.capacity()*SIZEOF_INT
                                                    +SIZEOF_INT);//intOut size field
        ByteBuffer readBuffer = newDirectByteBuffer(p4.capacity()*SIZEOF_INT+SIZEOF_LONG);

        readBuffer.putInt(rnd.nextInt());  //intOut buffer
        readBuffer.putInt(rnd.nextInt());
        readBuffer.putInt(rnd.nextInt());
        readBuffer.putInt(rnd.nextInt());

        readBuffer.putLong(ret0); //return
        readBuffer.rewind();

        DebugChannel channel = new DebugChannel(writeBuffer, readBuffer);
        Target client = createClientInterface(channel);

        // test 0
        long ret = client.test0(p0, p1, p2, p3, p4);
        assertEquals(ret0, ret);
        assertEquals(0, p4.position());

        writeBuffer.rewind();

        // header
        assertEquals(id, writeBuffer.get());
        assertEquals(0,  writeBuffer.getInt());

        // params
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
            assertEquals(readBuffer.getInt(), p4.get());
        }


        // test 1
        readBuffer.clear();
        writeBuffer.clear();

        client.test1();
        assertEquals(0, readBuffer.position());
        assertEquals(5, writeBuffer.position());
        writeBuffer.rewind();

        assertEquals(id, writeBuffer.get());
        assertEquals(1,  writeBuffer.getInt());


        // test 2
        readBuffer.clear();
        writeBuffer.clear();

        String string = "Hello World";
        byte[] bytes = string.getBytes();

        long ret3 = client.test2(-12, string, 52);
        assertEquals(SIZEOF_LONG, readBuffer.position());
        assertEquals(readBuffer.getLong(0), ret3);
        assertEquals(SIZEOF_HEADER+SIZEOF_INT+SIZEOF_INT+bytes.length+SIZEOF_INT, writeBuffer.position());
        writeBuffer.rewind();

        assertEquals(id, writeBuffer.get());
        assertEquals(2, writeBuffer.getInt());

        assertEquals(-12, writeBuffer.getInt());  // p0
        assertEquals(bytes.length, writeBuffer.getInt()); // p1 size

        for (int i = 0; i < bytes.length; i++) { // p1
            assertEquals(bytes[i], writeBuffer.get());
        }
        assertEquals(52, writeBuffer.getInt()); // p2

        // test3
        try{
            client.test3();
            fail();
        }catch (UnsupportedOperationException expected) {}

    }

    @Test
    public void serverTest() throws Exception {
        
        out.println("generator-server-test");

        final boolean[] called = new boolean[3];

        final long             t0p0 = rnd.nextLong();
        final int              t0p1 = rnd.nextInt();
        final NativeSizeBuffer t0p2 = allocateDirect(new long[]{rnd.nextLong(),rnd.nextLong(),rnd.nextLong()});
        final IntBuffer        t0p3 = newDirectIntBuffer(new int[]{rnd.nextInt(),rnd.nextInt(),rnd.nextInt()});
        final int[]            t0p4 = new int[]{rnd.nextInt(),rnd.nextInt(),rnd.nextInt()};
        final long             ret0 = rnd.nextLong();

        final int       t2p0 = rnd.nextInt();
        final String    t2p1 = "Hello World";
        final int       t2p2 = rnd.nextInt();
        final long      ret2 = rnd.nextLong();

        Target target = new Target() {
            
            @Override
            public long test0(long p0, int p1, NativeSizeBuffer p2, IntBuffer p3, IntBuffer out) {
                assertEquals(t0p0, p0);
                assertEquals(t0p1, p1);
                assertEquals(t0p2.remaining(), p2.remaining());
                for (int i = 0; i < t0p2.capacity(); i++) {
                    assertEquals(t0p2.get(i), p2.get(i));
                }
                assertEquals(t0p3.remaining(), p3.remaining());
                for (int i = 0; i < t0p3.capacity(); i++) {
                    assertEquals(t0p3.get(i), p3.get(i));
                }
                assertEquals(t0p4.length, out.remaining());
                for (int i = 0; i < t0p4.length; i++) {
                    out.put(i, t0p4[i]);
                }
                called(0);
                return ret0;
            }
            
            @Override
            public void test1() {
                called(1);
            }
            
            @Override
            public long test2(int p0, String p1, int p2) {
                assertEquals(t2p0, p0);
                assertEquals(t2p1, p1);
                assertEquals(t2p2, p2);
                called(2);
                return ret2;
            }

            @Override
            public void test3() {
                throw new RuntimeException("should not be called");
            }

            private void called(int test) {
                if(called[test]) fail(test + " already called");
                called[test] = true;
            }

        };
        
        CLHandler server = createServerHandler(target);

        ByteBuffer writeBuffer = newDirectByteBuffer(40000);
        ByteBuffer readBuffer = newDirectByteBuffer(40000);

        DebugChannel channel = new DebugChannel(writeBuffer, readBuffer);

        // test 0
        readBuffer.putLong(t0p0);
        readBuffer.putInt(t0p1);
        NetBuffers.putBuffer(readBuffer, t0p2.getBuffer());
        NetBuffers.putBuffer(readBuffer, t0p3);
        readBuffer.putInt(t0p4.length*SIZEOF_INT); // out
        readBuffer.rewind();
        
        server.handle(channel, 0);
        assertTrue(called[0]);
        writeBuffer.rewind();
        for (int i = 0; i < t0p4.length; i++) {
            assertEquals(t0p4[i], writeBuffer.getInt());
        }
        assertEquals(ret0, writeBuffer.getLong());


        // test 1
        writeBuffer.clear();
        readBuffer.clear();
        server.handle(channel, 1);
        assertTrue(called[1]);


        // test 2
        writeBuffer.clear();
        readBuffer.clear();
        readBuffer.putInt(t2p0);
        readBuffer.putInt(t2p1.length());
        readBuffer.put(t2p1.getBytes());
        readBuffer.putInt(t2p2);
        readBuffer.rewind();

        server.handle(channel, 2);
        assertTrue(called[2]);
        assertEquals(ret2, writeBuffer.getLong(0));


        // test 3
        try{
            server.handle(null, 3);
            fail();
        }catch (UnsupportedOperationException expected) {}
        
    }

    private Target createClientInterface(final DebugChannel channel) throws InvocationTargetException, SecurityException, IllegalAccessException, ClassNotFoundException, InstantiationException, NoSuchMethodException, IllegalArgumentException {
        Constructor<?> clientConstructor = Class.forName(clientPackage.replace('/', '.')+"."+clientImplName)
                            .getConstructor(RemoteNode.class);
        RemoteNode node = new RemoteNode(null, null, null) {
            @Override
            public ByteChannel connect() throws IOException {
                return channel;
            }
        };
        return (Target)clientConstructor.newInstance(node);
    }

    private CLHandler createServerHandler(Target target) throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException, SecurityException, NoSuchMethodException {
        Constructor<?> serverConstructor = Class.forName(serverPackage.replace('/', '.')+"."+serverImplName)
                                                .getConstructor(Target.class);
        return (CLHandler)serverConstructor.newInstance(target);

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


}
