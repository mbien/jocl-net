/*
 * Created on Wednesday, June 01 2011 19:13
 */
package com.mbien.generator.interfaces;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.CLErrorHandler;
import com.jogamp.opencl.llb.CLContextBinding;
import com.jogamp.opencl.llb.impl.CLImageFormatImpl;
import com.mbien.opencl.net.annotation.Out;
import com.mbien.opencl.net.annotation.Unsupported;
import java.nio.Buffer;
import java.nio.IntBuffer;

import static com.mbien.opencl.net.annotation.Unsupported.Kind.*;

/**
 *
 * @author Michael Bien
 */
public interface RemoteContextBinding extends CLContextBinding {

    @Override
    public int clGetContextInfo(long context, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public long clCreateContext(NativeSizeBuffer properties, NativeSizeBuffer devices, @Unsupported(NOOP) CLErrorHandler pfn_notify, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateContextFromType(NativeSizeBuffer properties, long device_type, @Unsupported(NOOP) CLErrorHandler pfn_notify, @Out IntBuffer errcode_ret);

    @Override
    public int clGetSupportedImageFormats(long context, long flags, int image_type, int num_entries, @Out CLImageFormatImpl image_formats, @Out IntBuffer num_image_formats);

    @Override
    public int clGetSupportedImageFormats(long context, long flags, int image_type, int num_entries, @Out CLImageFormatImpl image_formats, @Out int[] num_image_formats, int num_image_formats_offset);

    @Override
    public int clReleaseContext(long context);

    @Override
    public int clRetainContext(long context);

}
