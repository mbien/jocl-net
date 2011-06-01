/*
 * Created on Friday, May 27 2011 20:31
 */
package com.mbien.opencl.net.remote;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 *
 * @author Michael Bien
 */
public interface CLRemoteBinding {

    ByteBuffer getBuffer();

    ByteChannel getChannel();

}
