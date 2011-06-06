package com.mbien.generator;

import com.jogamp.common.nio.NativeSizeBuffer;
import com.mbien.opencl.net.annotation.Out;
import java.nio.IntBuffer;

/**
 *
 * @author Michael Bien
 */
public interface Target {

    public long test0(
        long longVal, int intVal,
        NativeSizeBuffer nativeSize,
        IntBuffer intIn, @Out IntBuffer intOut
    );
    
    public void test1();
    
    public long test2(int a, String foo, int b);

}
