/*
 * Created on Monday, May 23 2011 03:52
 */
package com.mbien.opencl.net.remote;

import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.spi.CLInfoAccessor;

/**
 *
 * @author Michael Bien
 */
public class CLRemoteDevice extends CLDevice {

    public CLRemoteDevice(CLRemotePlatform platform, RemoteNode node, long id) {
        super(null, platform, id);
    }

}
