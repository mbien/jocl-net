/*
 * Created on Monday, June 13 2011 20:53
 */
package com.mbien.generator.interfaces;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.jogamp.opencl.llb.CLSamplerBinding;
import com.mbien.opencl.net.annotation.Out;
import java.nio.Buffer;
import java.nio.IntBuffer;

/**
 *
 * @author Michael Bien
 */
public interface RemoteSamplerBinding extends CLSamplerBinding {

    @Override
    public long clCreateSampler(long context, int normalized_coords, int addressing_mode, int filter_mode, @Out IntBuffer errcode_ret);

    @Override
    public long clCreateSampler(long context, int normalized_coords, int addressing_mode, int filter_mode, @Out int[] errcode_ret, int errcode_ret_offset);

    @Override
    public int clGetSamplerInfo(long sampler, int param_name, long param_value_size, @Out Buffer param_value, @Out NativeSizeBuffer param_value_size_ret);

    @Override
    public int clReleaseSampler(long sampler);

    @Override
    public int clRetainSampler(long sampler);
}
