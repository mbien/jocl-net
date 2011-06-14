/*
 * Created on Thursday, June 09 2011 22:32
 */
package com.mbien.generator.interfaces;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.llb.CLBufferBinding;
import com.jogamp.opencl.llb.CLImageBinding;
import com.jogamp.opencl.llb.impl.CLImageFormatImpl;
import com.jogamp.opencl.llb.impl.CLMemObjectDestructorCallback;
import com.mbien.opencl.net.annotation.Out;
import com.mbien.opencl.net.annotation.Unsupported;
import java.nio.Buffer;
import java.nio.IntBuffer;

import static com.mbien.opencl.net.annotation.Unsupported.Kind.*;

/**
 *
 * @author Michael Bien
 */
public interface RemoteMemoryBinding extends CLBufferBinding, CLImageBinding {

    @Override
    public long clCreateBuffer(long context, long flags, long size, Buffer host_ptr, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateBuffer(long context, long flags, long size, Buffer host_ptr, @Out int[] errcode_ret, int errcode_ret_offset);

    @Override
    public long clCreateSubBuffer(long buffer, long flags, int buffer_create_type, Buffer buffer_create_info, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateSubBuffer(long buffer, long flags, int buffer_create_type, Buffer buffer_create_info, @Out int[] errcode_ret, int errcode_ret_offset);

    @Override
    public long clCreateImage2D(long context, long flags, CLImageFormatImpl image_format, long image_width, long image_height, long image_row_pitch, Buffer host_ptr, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateImage2D(long context, long flags, CLImageFormatImpl image_format, long image_width, long image_height, long image_row_pitch, Buffer host_ptr, @Out int[] errcode_ret, int errcode_ret_offset);

    @Override
    public long clCreateImage3D(long context, long flags, CLImageFormatImpl image_format, long image_width, long image_height, long image_depth, long image_row_pitch, long image_slice_pitch, Buffer host_ptr, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateImage3D(long context, long flags, CLImageFormatImpl image_format, long image_width, long image_height, long image_depth, long image_row_pitch, long image_slice_pitch, Buffer host_ptr, @Out int[] errcode_ret, int errcode_ret_offset);

    @Override
    public int clGetImageInfo(long image, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clGetMemObjectInfo(long memobj, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    @Unsupported(UOE)
    public int clSetMemObjectDestructorCallback(long memObjID, CLMemObjectDestructorCallback cb);

    @Override
    public int clReleaseMemObject(long memobj);

    @Override
    public int clRetainMemObject(long memobj);

}
