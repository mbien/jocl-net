/*
 * Created on Saturday, May 28 2011 14:57
 */
package com.mbien.opencl.net;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opencl.llb.CL;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static com.jogamp.common.nio.Buffers.*;

/**
 *
 * @author Michael Bien
 */
public abstract class CLHandler {

    private final ByteBuffer bb;
    
    protected final CL cl;

    public CLHandler(CL cl) {
        this.cl = cl;
        this.bb = Buffers.newDirectByteBuffer(40000);
    }

    public ByteBuffer getBuffer() {
        bb.clear();
        return bb;
    }
    
    protected abstract void handle(SocketChannel channel, int methodID) throws IOException;

    protected int readInt(SocketChannel channel, ByteBuffer buffer) throws IOException {
        buffer.limit(Buffers.SIZEOF_INT);
        channel.read(buffer);
        buffer.rewind();
        return buffer.getInt(0);
    }

    protected long readLong(SocketChannel channel, ByteBuffer buffer) throws IOException {
        buffer.limit(Buffers.SIZEOF_LONG);
        channel.read(buffer);
        buffer.rewind();
        return buffer.getLong(0);
    }

    protected void readBytes(SocketChannel channel, NativeSizeBuffer buffer) throws IOException {
        ByteBuffer bb = buffer.getBuffer();
        channel.read(bb);
        bb.clear();
    }

    protected void readInts(SocketChannel channel, ByteBuffer buffer) throws IOException {
        channel.read(buffer);
        buffer.clear();
    }

    protected void writeInt(SocketChannel channel, ByteBuffer buffer, int value) throws IOException {
        buffer.limit(SIZEOF_INT);
        buffer.putInt(0, value);
        buffer.rewind();
        channel.write(buffer);
        buffer.clear();
    }

    protected void writeLong(SocketChannel channel, ByteBuffer buffer, long value) throws IOException {
        buffer.limit(SIZEOF_LONG);
        buffer.putLong(0, value);
        buffer.rewind();
        channel.write(buffer);
        buffer.clear();
    }
    
}
