/*
 * Created on Friday, May 27 2011 20:31
 */
package com.mbien.opencl.net.remote;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.ByteChannel;

/**
 *
 * @author Michael Bien
 */
public interface CLRemoteBinding {

    ByteBuffer getBuffer();

    ByteChannel getChannel();

    void putBytes(ByteBuffer target, ByteBuffer source);

    void putInts(ByteBuffer target, IntBuffer source);

    void readBuffer(ByteChannel channel, IntBuffer target, ByteBuffer buffer) throws IOException;

}
