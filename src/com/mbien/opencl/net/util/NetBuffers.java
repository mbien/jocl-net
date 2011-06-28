/*
 * Created on Wednesday, June 01 2011 03:46
 */
package com.mbien.opencl.net.util;

import com.jogamp.common.nio.NativeBuffer;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static com.jogamp.common.nio.Buffers.*;
import static java.lang.Math.*;

/**
 * Buffer utilities primary intended for buffer serialization.
 * @author Michael Bien
 */
public class NetBuffers {

    private NetBuffers() { }


    public static byte readByte(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_BYTE);
        read(channel, dest);
        dest.rewind();
        return dest.get(0);
    }

    public static int readShort(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_SHORT);
        read(channel, dest);
        dest.rewind();
        return dest.getShort(0);
    }

    public static int readInt(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_INT);
        read(channel, dest);
        dest.rewind();
        return dest.getInt(0);
    }

    public static long readLong(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_LONG);
        read(channel, dest);
        dest.rewind();
        return dest.getLong(0);
    }

    public static float readFloat(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_FLOAT);
        read(channel, dest);
        dest.rewind();
        return dest.getFloat(0);
    }

    public static double readDouble(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        dest.limit(SIZEOF_DOUBLE);
        read(channel, dest);
        dest.rewind();
        return dest.getDouble(0);
    }


    public static ByteBuffer readBuffer(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        read(channel, dest);
        dest.clear();
        return dest;
    }

    public static <T extends NativeBuffer> T readBuffer(ReadableByteChannel channel, T dest) throws IOException {
        readBuffer(channel, dest.getBuffer());
        return dest;
    }

    public static ShortBuffer readBuffer(ReadableByteChannel channel, ShortBuffer dest, ByteBuffer temp) throws IOException {
        do {
            int remaining = dest.remaining()*SIZEOF_SHORT;
            if(temp.capacity() < remaining) {
                temp.clear();
            }else{
                temp.rewind().limit(remaining);
            }
            read(channel, temp);
            temp.rewind();
            do{
                dest.put(temp.getShort());
            }while(temp.hasRemaining());
        }while(dest.hasRemaining());
        return dest;
    }
    
    public static IntBuffer readBuffer(ReadableByteChannel channel, IntBuffer dest, ByteBuffer temp) throws IOException {
        do {
            int remaining = dest.remaining()*SIZEOF_INT;
            if(temp.capacity() < remaining) {
                temp.clear();
            }else{
                temp.rewind().limit(remaining);
            }
            read(channel, temp);
            temp.rewind();
            do{
                dest.put(temp.getInt());
            }while(temp.hasRemaining());
        }while(dest.hasRemaining());
        return dest;
    }

    public static LongBuffer readBuffer(ReadableByteChannel channel, LongBuffer dest, ByteBuffer temp) throws IOException {
        do {
            int remaining = dest.remaining()*SIZEOF_LONG;
            if(temp.capacity() < remaining) {
                temp.clear();
            }else{
                temp.rewind().limit(remaining);
            }
            read(channel, temp);
            temp.rewind();
            do{
                dest.put(temp.getLong());
            }while(temp.hasRemaining());
        }while(dest.hasRemaining());
        return dest;
    }

    public static FloatBuffer readBuffer(ReadableByteChannel channel, FloatBuffer dest, ByteBuffer temp) throws IOException {
        do {
            int remaining = dest.remaining()*SIZEOF_FLOAT;
            if(temp.capacity() < remaining) {
                temp.clear();
            }else{
                temp.rewind().limit(remaining);
            }
            read(channel, temp);
            temp.rewind();
            do{
                dest.put(temp.getFloat());
            }while(temp.hasRemaining());
        }while(dest.hasRemaining());
        return dest;
    }

    public static DoubleBuffer readBuffer(ReadableByteChannel channel, DoubleBuffer dest, ByteBuffer temp) throws IOException {
        do {
            int remaining = dest.remaining()*SIZEOF_DOUBLE;
            if(temp.capacity() < remaining) {
                temp.clear();
            }else{
                temp.rewind().limit(remaining);
            }
            read(channel, temp);
            temp.rewind();
            do{
                dest.put(temp.getDouble());
            }while(temp.hasRemaining());
        }while(dest.hasRemaining());
        return dest;
    }

    public static <T extends Buffer> T readBuffer(ReadableByteChannel channel, T dest, ByteBuffer temp) throws IOException {
        if(dest instanceof ByteBuffer) {
            return (T) readBuffer(channel, (ByteBuffer)dest);
        }else if(dest instanceof ShortBuffer) {
            return (T) readBuffer(channel, (ShortBuffer)dest, temp);
        }else if(dest instanceof IntBuffer) {
            return (T) readBuffer(channel, (IntBuffer)dest, temp);
        }else if(dest instanceof LongBuffer) {
            return (T) readBuffer(channel, (LongBuffer)dest, temp);
        }else if(dest instanceof FloatBuffer) {
            return (T) readBuffer(channel, (FloatBuffer)dest, temp);
        }else if(dest instanceof DoubleBuffer) {
            return (T) readBuffer(channel, (DoubleBuffer)dest, temp);
        }else{
            throw new IllegalArgumentException("unsupported buffer "+dest);
        }
    }

    public static String readString(ReadableByteChannel channel, int length) throws IOException {
        if(length < 0) {
            return null;
        }else if(length == 0) {
            return "";
        }

        ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.nativeOrder());
        read(channel, buffer);
        buffer.rewind();
        return new String(buffer.array());
    }

    public static byte[] readArray(ReadableByteChannel channel, byte[] array, ByteBuffer temp) throws IOException {
        temp.rewind();
        temp.limit(array.length*SIZEOF_BYTE);
        read(channel, temp);
        for (int i = 0; i < array.length; i++) {
            array[i] = temp.get(i*SIZEOF_BYTE);
        }
        return array;
    }

    public static int[] readArray(ReadableByteChannel channel, int[] array, ByteBuffer temp) throws IOException {
        temp.rewind();
        temp.limit(array.length*SIZEOF_INT);
        read(channel, temp);
        for (int i = 0; i < array.length; i++) {
            array[i] = temp.getInt(i*SIZEOF_INT);
        }
        return array;
    }

    public static long[] readArray(ReadableByteChannel channel, long[] array, ByteBuffer temp) throws IOException {
        temp.rewind();
        temp.limit(array.length*SIZEOF_LONG);
        read(channel, temp);
        for (int i = 0; i < array.length; i++) {
            array[i] = temp.getLong(i*SIZEOF_LONG);
        }
        return array;
    }

    public static float[] readArray(ReadableByteChannel channel, float[] array, ByteBuffer temp) throws IOException {
        temp.rewind();
        temp.limit(array.length*SIZEOF_FLOAT);
        read(channel, temp);
        for (int i = 0; i < array.length; i++) {
            array[i] = temp.getFloat(i*SIZEOF_FLOAT);
        }
        return array;
    }

    public static double[] readArray(ReadableByteChannel channel, double[] array, ByteBuffer temp) throws IOException {
        temp.rewind();
        temp.limit(array.length*SIZEOF_DOUBLE);
        read(channel, temp);
        for (int i = 0; i < array.length; i++) {
            array[i] = temp.getDouble(i*SIZEOF_DOUBLE);
        }
        return array;
    }

    public static void writeShort(WritableByteChannel channel, ByteBuffer temp, short value) throws IOException {
        temp.limit(SIZEOF_SHORT);
        temp.putInt(0, value);
        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    public static void writeInt(WritableByteChannel channel, ByteBuffer temp, int value) throws IOException {
        temp.limit(SIZEOF_INT);
        temp.putInt(0, value);
        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    public static void writeLong(WritableByteChannel channel, ByteBuffer temp, long value) throws IOException {
        temp.limit(SIZEOF_LONG);
        temp.putLong(0, value);
        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    public static void writeArray(WritableByteChannel channel, ByteBuffer temp, byte[] value) throws IOException {
        temp.limit(SIZEOF_BYTE*value.length);
        for (int i = 0; i < value.length; i++) {
            temp.put(value[i]);
        }
        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    public static void writeArray(WritableByteChannel channel, ByteBuffer temp, int[] value) throws IOException {
        temp.limit(SIZEOF_INT*value.length);
        for (int i = 0; i < value.length; i++) {
            temp.putInt(value[i]);
        }

        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    public static void writeArray(WritableByteChannel channel, ByteBuffer temp, long[] value) throws IOException {
        temp.limit(SIZEOF_LONG*value.length);
        for (int i = 0; i < value.length; i++) {
            temp.putLong(value[i]);
        }
        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    public static void writeArray(WritableByteChannel channel, ByteBuffer temp, float[] value) throws IOException {
        temp.limit(SIZEOF_FLOAT*value.length);
        for (int i = 0; i < value.length; i++) {
            temp.putFloat(value[i]);
        }
        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    public static void writeArray(WritableByteChannel channel, ByteBuffer temp, double[] value) throws IOException {
        temp.limit(SIZEOF_DOUBLE*value.length);
        for (int i = 0; i < value.length; i++) {
            temp.putDouble(value[i]);
        }
        temp.rewind();
        write(channel, temp);
        temp.clear();
    }

    private static void writeNullBuffer(ByteBuffer buffer, WritableByteChannel channel) throws IOException {
        buffer.putInt(0);
        write(channel, buffer);
        buffer.clear();
    }

    public static void writeLargeBuffer(WritableByteChannel channel, ByteBuffer buffer, Buffer source) throws IOException {
        if(source == null) {
            writeNullBuffer(buffer, channel);
        }else if(source instanceof ByteBuffer) {
            writeLargeBuffer(channel, buffer, (ByteBuffer) source);
        }else if(source instanceof ShortBuffer) {
            writeLargeBuffer(channel, buffer, (ShortBuffer) source);
        }else if(source instanceof IntBuffer) {
            writeLargeBuffer(channel, buffer, (IntBuffer) source);
        }else if(source instanceof LongBuffer) {
            writeLargeBuffer(channel, buffer, (LongBuffer) source);
        }else if(source instanceof FloatBuffer) {
            writeLargeBuffer(channel, buffer, (FloatBuffer) source);
        }else if(source instanceof DoubleBuffer) {
            writeLargeBuffer(channel, buffer, (DoubleBuffer) source);
        }else{
            throw new IllegalArgumentException("unsupported buffer "+source);
        }
    }

    public static void writeLargeBuffer(WritableByteChannel channel, ByteBuffer buffer, ByteBuffer source) throws IOException {
        if(source == null) {
            writeNullBuffer(buffer, channel);
        }else{
            buffer.putInt(source.remaining());
            buffer.flip();
            write(channel, buffer);

            source.mark();
            write(channel, source);
            source.reset();
            buffer.clear();
        }
    }

    public static void writeLargeBuffer(WritableByteChannel channel, ByteBuffer buffer, ShortBuffer source) throws IOException {
        if(source == null) {
            writeNullBuffer(buffer, channel);
        }else{
            buffer.putInt(source.remaining()*SIZEOF_SHORT);
            source.mark();
            do{
                int remaining = min(buffer.remaining()/SIZEOF_SHORT, source.remaining());
                for (int i = 0; i < remaining; i++) {
                    buffer.putShort(source.get());
                }
                buffer.flip();
                write(channel, buffer);
                buffer.clear();
            }while(source.hasRemaining());
            source.reset();
        }
    }

    public static void writeLargeBuffer(WritableByteChannel channel, ByteBuffer buffer, IntBuffer source) throws IOException {
        if(source == null) {
            writeNullBuffer(buffer, channel);
        }else{
            buffer.putInt(source.remaining()*SIZEOF_INT);
            source.mark();
            do{
                int remaining = min(buffer.remaining()/SIZEOF_INT, source.remaining());
                for (int i = 0; i < remaining; i++) {
                    buffer.putInt(source.get());
                }
                buffer.flip();
                write(channel, buffer);
                buffer.clear();
            }while(source.hasRemaining());
            source.reset();
        }
    }

    public static void writeLargeBuffer(WritableByteChannel channel, ByteBuffer buffer, LongBuffer source) throws IOException {
        if(source == null) {
            writeNullBuffer(buffer, channel);
        }else{
            buffer.putInt(source.remaining()*SIZEOF_LONG);
            source.mark();
            do{
                int remaining = min(buffer.remaining()/SIZEOF_LONG, source.remaining());
                for (int i = 0; i < remaining; i++) {
                    buffer.putLong(source.get());
                }
                buffer.flip();
                write(channel, buffer);
                buffer.clear();
            }while(source.hasRemaining());
            source.reset();
        }
    }

    public static void writeLargeBuffer(WritableByteChannel channel, ByteBuffer buffer, FloatBuffer source) throws IOException {
        if(source == null) {
            writeNullBuffer(buffer, channel);
        }else{
            buffer.putInt(source.remaining()*SIZEOF_FLOAT);
            source.mark();
            do{
                int remaining = min(buffer.remaining()/SIZEOF_FLOAT, source.remaining());
                for (int i = 0; i < remaining; i++) {
                    buffer.putFloat(source.get());
                }
                buffer.flip();
                write(channel, buffer);
                buffer.clear();
            }while(source.hasRemaining());
            source.reset();
        }
    }

    public static void writeLargeBuffer(WritableByteChannel channel, ByteBuffer buffer, DoubleBuffer source) throws IOException {
        if(source == null) {
            writeNullBuffer(buffer, channel);
        }else{
            buffer.putInt(source.remaining()*SIZEOF_DOUBLE);
            source.mark();
            do{
                int remaining = min(buffer.remaining()/SIZEOF_DOUBLE, source.remaining());
                for (int i = 0; i < remaining; i++) {
                    buffer.putDouble(source.get());
                }
                buffer.flip();
                write(channel, buffer);
                buffer.clear();
            }while(source.hasRemaining());
            source.reset();
        }
    }

    public static void putBuffer(ByteBuffer dest, Buffer source) {
        if(source == null) {
            putBuffer(dest, (ByteBuffer)null);
        }else if(source instanceof ByteBuffer) {
            putBuffer(dest, (ByteBuffer)source);
        }else if(source instanceof IntBuffer) {
            putBuffer(dest, (IntBuffer)source);
        }else if(source instanceof LongBuffer) {
            putBuffer(dest, (LongBuffer)source);
        }else if(source instanceof FloatBuffer) {
            putBuffer(dest, (FloatBuffer)source);
        }else if(source instanceof DoubleBuffer) {
            putBuffer(dest, (DoubleBuffer)source);
        }else{
            throw new IllegalArgumentException("unsupported buffer "+source);
        }
    }

    public static void putBuffer(ByteBuffer dest, NativeBuffer source) {
        if(source == null) {
            putBuffer(dest, (ByteBuffer)null);
        }else{
            putBuffer(dest, source.getBuffer());
        }
    }

    public static void putBuffer(ByteBuffer dest, ByteBuffer source) {
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

    public static void putBuffer(ByteBuffer dest, IntBuffer source) {
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

    public static void putBuffer(ByteBuffer dest, LongBuffer source) {
        if(source == null) {
            dest.putInt(0);
        }else{
            dest.putInt(source.remaining() * SIZEOF_LONG);

            int sourcepos = source.position();
            for (int i = 0; i < source.remaining(); i++) {
                dest.putLong(source.get(sourcepos + i));
            }
        }
    }

    public static void putBuffer(ByteBuffer dest, FloatBuffer source) {
        if(source == null) {
            dest.putInt(0);
        }else{
            dest.putInt(source.remaining() * SIZEOF_FLOAT);

            int sourcepos = source.position();
            for (int i = 0; i < source.remaining(); i++) {
                dest.putFloat(source.get(sourcepos + i));
            }
        }
    }

    public static void putBuffer(ByteBuffer dest, DoubleBuffer source) {
        if(source == null) {
            dest.putInt(0);
        }else{
            dest.putInt(source.remaining() * SIZEOF_DOUBLE);

            int sourcepos = source.position();
            for (int i = 0; i < source.remaining(); i++) {
                dest.putDouble(source.get(sourcepos + i));
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

    public static void read(ReadableByteChannel channel, ByteBuffer dest) throws IOException {
        int bytes;
        do{
            bytes = channel.read(dest);
        }while(dest.hasRemaining() && bytes >= 0);
        if(bytes < 0 && dest.hasRemaining()) {
            throw new IOException("end of stream");
        }
    }

    public static void write(WritableByteChannel channel, ByteBuffer source) throws IOException {
        int bytes;
        do{
            bytes = channel.write(source);
        }while(source.hasRemaining() && bytes >= 0);
        if(bytes < 0 && source.hasRemaining()) {
            throw new IOException("end of stream");
        }
    }

}
