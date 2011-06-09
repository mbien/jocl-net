/*
 * Created on Thursday, June 09 2011 03:05
 */
package com.mbien.generator.interfaces;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.llb.CLKernelBinding;
import com.mbien.opencl.net.annotation.Out;
import java.nio.Buffer;
import java.nio.IntBuffer;

/**
 *
 * @author Michael Bien
 */
public interface RemoteKernelBinding extends CLKernelBinding {

    @Override
    public long clCreateKernel(long program, String kernel_name, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateKernel(long program, String kernel_name, int[] errcode_ret, int errcode_ret_offset);

    @Override
    public int clCreateKernelsInProgram(long program, int num_kernels, @Out NativeSizeBuffer kernels, @Out IntBuffer num_kernels_ret);

    @Override
    public int clGetKernelInfo(long kernel, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clGetKernelWorkGroupInfo(long kernel, long device, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clSetKernelArg(long kernel, int arg_index, long arg_size, Buffer arg_value);

    @Override
    public int clReleaseKernel(long kernel);

    @Override
    public int clRetainKernel(long kernel);
}
