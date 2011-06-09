/*
 * Created on Wednesday, June 01 2011 03:46
 */
package com.mbien.opencl.net.util;

import com.jogamp.common.nio.NativeBuffer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static com.jogamp.common.nio.Buffers.*;

/**
 * Buffer utilities primary intended for buffer serialization.
 * @author Michael Bien
 */
public class NetBuffers {

    private NetBuffers() { }


    public static int readInt(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_INT);
        channel.read(dest);
        dest.rewind();
        return dest.getInt(0);
    }

    public static long readLong(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_LONG);
        channel.read(dest);
        dest.rewind();
        return dest.getLong(0);
    }

    public static void readBytes(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        channel.read(dest);
        dest.clear();
    }

    public static void readBytes(ReadableByteChannel channel, NativeBuffer dest) throws IOException {
        readBytes(channel, dest.getBuffer());
    }

    public static void readInts(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        channel.read(dest);
        dest.clear();
    }

    public static void readBuffer(ReadableByteChannel channel, IntBuffer dest, ByteBuffer temp) throws IOException {
        temp.rewind();
        temp.limit(dest.remaining()*SIZEOF_INT);
        channel.read(temp);
        for(int i = 0; i < dest.remaining(); i++) {
            dest.put(i, temp.getInt(i*SIZEOF_INT));
        }
    }

    public static String readString(ReadableByteChannel channel, int length) throws IOException {
        if(length < 0) {
            return null;
        }else if(length == 0) {
            return "";
        }

        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.nativeOrder());
        channel.read(buffer);
        buffer.rewind();
        return new String(buffer.array());
    }

    public static void writeInt(WritableByteChannel channel, ByteBuffer temp, int value) throws IOException {
        temp.limit(SIZEOF_INT);
        temp.putInt(0, value);
        temp.rewind();
        channel.write(temp);
        temp.clear();
    }

    public static void writeLong(WritableByteChannel channel, ByteBuffer temp, long value) throws IOException {
        temp.limit(SIZEOF_LONG);
        temp.putLong(0, value);
        temp.rewind();
        channel.write(temp);
        temp.clear();
    }

    public static void putBytes(ByteBuffer dest, NativeBuffer source) {
        if(source == null) {
            putBytes(dest, (ByteBuffer)null);
        }else{
            putBytes(dest, source.getBuffer());
        }
    }

    public static void putBytes(ByteBuffer dest, ByteBuffer source) {
        if(source == null) {
            dest.putInt(0);
        }else{
            dest.putInt(source.remaining());

            int sourcepos = source.position();
            for (int i = 0; i < source.remaining(); i++) {
                dest.put(source.get(sourcepos + i));
            }
        }
    }

    public static void putInts(ByteBuffer dest, IntBuffer source) {
        if(source == null) {
            dest.putInt(0);
        }else{
            dest.putInt(source.remaining() * SIZEOF_INT);

            int sourcepos = source.position();
            for (int i = 0; i < source.remaining(); i++) {
                dest.putInt(source.get(sourcepos + i));
            }
        }
    }

    public static void putString(ByteBuffer dest, String string) {
        if(string == null) {
            dest.putInt(-1);
        }else if(string.length() == 0) {
            dest.putInt(0);
        }else{
            byte[] bytes = string.getBytes();
            dest.putInt(bytes.length);
            dest.put(bytes);
        }
    }

}
