/*
 * Created on Thursday, June 16 2011 01:18
 */
package com.mbien.opencl.net.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.Test;

import static junit.framework.Assert.*;
import static com.jogamp.common.nio.Buffers.*;

/**
 *
 * @author Michael Bien
 */
public class NetBuffersTest {


    @Test
    public void largeBuffersTest() throws IOException {

        Random rnd = new Random();

        for (int i = 0; i < 10; i++) {

            {
                int[] ints = rndInts(rnd, i+1);

                ByteBuffer wb = newDirectByteBuffer(11*SIZEOF_INT+SIZEOF_INT);
                ByteBuffer buffer = newDirectByteBuffer(16);
                DebugChannel debugChannel = new DebugChannel(wb, null);

                NetBuffers.writeLargeBuffer(debugChannel, buffer, newDirectIntBuffer(ints));
                assertEquals(ints.length*SIZEOF_INT+SIZEOF_INT, wb.position());

                wb.rewind();
                assertContains(ints, wb);
                
            }
            {
                long[] longs = rndLongs(rnd, i+1);

                ByteBuffer wb = newDirectByteBuffer(11*SIZEOF_LONG+SIZEOF_INT);
                ByteBuffer buffer = newDirectByteBuffer(32);
                DebugChannel debugChannel = new DebugChannel(wb, null);

                NetBuffers.writeLargeBuffer(debugChannel, buffer, newDirectLongBuffer(longs));
                assertEquals(longs.length*SIZEOF_LONG+SIZEOF_INT, wb.position());

                wb.rewind();
                assertContains(longs, wb);

            }
            {
                byte[] bytes = rndBytes(rnd, i+1);

                ByteBuffer wb = newDirectByteBuffer(11*SIZEOF_BYTE+SIZEOF_INT);
                ByteBuffer buffer = newDirectByteBuffer(32);
                DebugChannel debugChannel = new DebugChannel(wb, null);

                NetBuffers.writeLargeBuffer(debugChannel, buffer, newDirectByteBuffer(bytes));
                assertEquals(bytes.length*SIZEOF_BYTE+SIZEOF_INT, wb.position());

                wb.rewind();
                assertContains(bytes, wb);

            }
        }


    }

    public void assertContains(byte[] array, ByteBuffer buffer) {
        buffer.mark();
        assertEquals(array.length*SIZEOF_BYTE, buffer.getInt());
        for (int i : array) {
            assertEquals(i, buffer.get());
        }
        buffer.rewind();
    }

    public void assertContains(int[] array, ByteBuffer buffer) {
        buffer.mark();
        assertEquals(array.length*SIZEOF_INT, buffer.getInt());
        for (int i : array) {
            assertEquals(i, buffer.getInt());
        }
        buffer.rewind();
    }

    public void assertContains(long[] array, ByteBuffer buffer) {
        buffer.mark();
        assertEquals(array.length*SIZEOF_LONG, buffer.getInt());
        for (long i : array) {
            assertEquals(i, buffer.getLong());
        }
        buffer.rewind();
    }

    private byte[] rndBytes(Random rnd, int size) {
        byte[] array = new byte[size];
        rnd.nextBytes(array);
        return array;
    }

    private int[] rndInts(Random rnd, int size) {
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = rnd.nextInt();
        }
        return array;
    }

    private long[] rndLongs(Random rnd, int size) {
        long[] array = new long[size];
        for (int i = 0; i < size; i++) {
            array[i] = rnd.nextLong();
        }
        return array;
    }

    private float[] rndFloats(Random rnd, int size) {
        float[] array = new float[size];
        for (int i = 0; i < size; i++) {
            array[i] = rnd.nextFloat();
        }
        return array;
    }

    private double[] rndDouble(Random rnd, int size) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++) {
            array[i] = rnd.nextDouble();
        }
        return array;
    }

}
