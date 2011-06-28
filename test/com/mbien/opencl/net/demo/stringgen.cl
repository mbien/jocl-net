/*
 *  Created on Saturday, June 10 2011
 *  @author Michael Bien
 */

//#pragma OPENCL EXTENSION cl_amd_printf : enable

// works only for about 15 chars until long gets to short
kernel void gen(global short* message, const int offset, const int size, const long value, const uint base) {

    int id = get_global_id(0);
    int msg = id*size;

    long v = value+id;
    for (int i = size-1; i >= 0; i--) {
        long k = v/base;
        message[msg + i] = (short) (v-base*k + offset);
        v = k;
    }

}

kernel void contains(global short* data, global short* item, const int size, global int* output) {

    int index = get_global_id(0) * size;

    for(int i = 0; i < size; i++) {

        if(data[index+i] != item[i]) {
            return;
        }
    }

    output[0] = get_global_id(0);

}
