/*
 * Created on Thursday, May 19 2011 21:40
 */
package com.mbien.opencl.net.remote;

import com.mbien.opencl.net.annotation.Out;
import com.mbien.opencl.net.annotation.In;
import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLErrorHandler;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.llb.CLContextBinding;
import com.jogamp.opencl.llb.impl.CLImageFormatImpl;
import java.nio.Buffer;
import java.nio.IntBuffer;


/**
 *
 * @author Michael Bien
 */
public class CLRemotePlatform extends CLPlatform {

    private final CLRemoteContextBinding contextBinding;

    private final RemoteNode node;

    public CLRemotePlatform(long id, RemoteNode node, CLRemoteAccessorFactory factory) {
        super(id, factory);
        this.node = node;
        this.contextBinding = new CLRemoteContextBinding(node);
    }

    @Override
    protected CLDevice createDevice(long id) {
        return new CLRemoteDevice(this, node, id);
    }

    @Override
    protected CLContextBinding getContextBinding() {
        return contextBinding;
    }


    public interface RemoteContextBinding extends CLContextBinding {

        @Override
        public int clGetContextInfo(long context, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

        @Override
        public long clCreateContext(@In NativeSizeBuffer properties, @In NativeSizeBuffer devices, CLErrorHandler pfn_notify, @Out IntBuffer errcode_ret);

        @Override
        public long clCreateContextFromType(@In NativeSizeBuffer properties, long device_type, CLErrorHandler pfn_notify, @Out IntBuffer errcode_ret);

        @Override
        public int clGetSupportedImageFormats(long context, long flags, int image_type, int num_entries, CLImageFormatImpl image_formats, IntBuffer num_image_formats);

        @Override
        public int clGetSupportedImageFormats(long context, long flags, int image_type, int num_entries, CLImageFormatImpl image_formats, int[] num_image_formats, int num_image_formats_offset);

        @Override
        public int clReleaseContext(long context);

        @Override
        public int clRetainContext(long context);

    }


}
