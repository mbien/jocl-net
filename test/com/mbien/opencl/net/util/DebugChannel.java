/*
 * Created on Saturday, June 04 2011 00:20
 */
package com.mbien.opencl.net.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 *
 * @author Michael Bien
 */
public class DebugChannel implements ByteChannel {

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
        if (rb.remaining() < remaining) {
            throw new RuntimeException("readbuffer to small, requested " + remaining + "bytes");
        }
        rb.limit(rb.position() + remaining);
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
