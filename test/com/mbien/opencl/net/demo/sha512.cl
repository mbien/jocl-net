/*
*  Created on Saturday, June 10 2011
*  @author Michael Bien
*/

ulong rotateRight(ulong i, uint distance) {
    return (i >> distance) | (i << -distance);
}

ulong sigma0(ulong l) {
    return rotateRight(l, 1) ^ rotateRight(l, 8) ^ (l >> 7);
}

ulong sigma1(ulong l) {
    return rotateRight(l, 19) ^ rotateRight(l, 61) ^ (l >> 6);
}

ulong sum0(ulong a) {
    return rotateRight(a, 28) ^ rotateRight(a, 34) ^ rotateRight(a, 39);
}

ulong sum1(ulong e) {
    return rotateRight(e, 14) ^ rotateRight(e, 18) ^ rotateRight(e, 41);
}

ulong ch(ulong e, ulong f, ulong g) {
    return (e & f) ^ ((~e) & g);
}

ulong maj(ulong a, ulong b, ulong c) {
    return (a & b) ^ (a & c) ^ (b & c);
}

constant ulong K[80] = {
    0x428A2F98D728AE22L, 0x7137449123EF65CDL, 0xB5C0FBCFEC4D3B2FL,
    0xE9B5DBA58189DBBCL, 0x3956C25BF348B538L, 0x59F111F1B605D019L,
    0x923F82A4AF194F9BL, 0xAB1C5ED5DA6D8118L, 0xD807AA98A3030242L,
    0x12835B0145706FBEL, 0x243185BE4EE4B28CL, 0x550C7DC3D5FFB4E2L,
    0x72BE5D74F27B896FL, 0x80DEB1FE3B1696B1L, 0x9BDC06A725C71235L,
    0xC19BF174CF692694L, 0xE49B69C19EF14AD2L, 0xEFBE4786384F25E3L,
    0x0FC19DC68B8CD5B5L, 0x240CA1CC77AC9C65L, 0x2DE92C6F592B0275L,
    0x4A7484AA6EA6E483L, 0x5CB0A9DCBD41FBD4L, 0x76F988DA831153B5L,
    0x983E5152EE66DFABL, 0xA831C66D2DB43210L, 0xB00327C898FB213FL,
    0xBF597FC7BEEF0EE4L, 0xC6E00BF33DA88FC2L, 0xD5A79147930AA725L,
    0x06CA6351E003826FL, 0x142929670A0E6E70L, 0x27B70A8546D22FFCL,
    0x2E1B21385C26C926L, 0x4D2C6DFC5AC42AEDL, 0x53380D139D95B3DFL,
    0x650A73548BAF63DEL, 0x766A0ABB3C77B2A8L, 0x81C2C92E47EDAEE6L,
    0x92722C851482353BL, 0xA2BFE8A14CF10364L, 0xA81A664BBC423001L,
    0xC24B8B70D0F89791L, 0xC76C51A30654BE30L, 0xD192E819D6EF5218L,
    0xD69906245565A910L, 0xF40E35855771202AL, 0x106AA07032BBD1B8L,
    0x19A4C116B8D2D0C8L, 0x1E376C085141AB53L, 0x2748774CDF8EEB99L,
    0x34B0BCB5E19B48A8L, 0x391C0CB3C5C95A63L, 0x4ED8AA4AE3418ACBL,
    0x5B9CCA4F7763E373L, 0x682E6FF3D6B2B8A3L, 0x748F82EE5DEFB2FCL,
    0x78A5636F43172F60L, 0x84C87814A1F0AB72L, 0x8CC702081A6439ECL,
    0x90BEFFFA23631E28L, 0xA4506CEBDE82BDE9L, 0xBEF9A3F7B2C67915L,
    0xC67178F2E372532BL, 0xCA273ECEEA26619CL, 0xD186B8C721C0C207L,
    0xEADA7DD6CDE0EB1EL, 0xF57D4F7FEE6ED178L, 0x06F067AA72176FBAL,
    0x0A637DC5A2C898A6L, 0x113F9804BEF90DAEL, 0x1B710B35131C471BL,
    0x28DB77F523047D84L, 0x32CAAB7B40C72493L, 0x3C9EBE0A15C9BEBCL,
    0x431D67C49C100D4CL, 0x4CC5D4BECB3E42B6L, 0x597F299CFC657E2AL,
    0x5FCB6FAB3AD6FAECL, 0x6C44198C4A475817L
};

kernel void pad(global short* data, global short* paddedOutput, const int origLength) {

    uint tailLength = origLength % 128;
    uint padLength  = 128 - tailLength;

    short thePad[128];
    for (int i = 0; i < 128; i++) {
        thePad[i] = 0;
    }

    thePad[0] = (short) 0x80;
    ulong lengthInBits = origLength * 8;
    for (uint i = 0; i < 8; i++) {
        thePad[padLength - 1 - i] = (short) ((lengthInBits >> (8 * i)) & 0xFFL);
    }


    int inputOffset  = get_global_id(0) * origLength;
    int outputOffset = get_global_id(0) * 128;

    for (int i = 0; i < origLength; i++) {
        paddedOutput[outputOffset+i] = data[inputOffset+i];
    }
    for (int i = 0; i < padLength; i++) {
        paddedOutput[outputOffset+origLength+i] = thePad[i];
    }
}


kernel void sha512(global short* message, global short* digest, global int* range) {

    int id = get_global_id(0);
    uint offset = range[id*2  ];
    uint length = range[id*2+1];

    ulong H[8] = {
        0x6A09E667F3BCC908L, 0xBB67AE8584CAA73BL,
        0x3C6EF372FE94F82BL, 0xA54FF53A5F1D36F1L,
        0x510E527FADE682D1L, 0x9B05688C2B3E6C1FL,
        0x1F83D9ABFB41BD6BL, 0x5BE0CD19137E2179L
    };

    uint block[128];

    for (uint i = 0; i < length / 128; i++) {

        for (uint j = 0; j < 128; j++) {
            block[j] = message[offset+128*i+j];
        }

        ulong words[80];

        for (uint j = 0; j < 16; j++) {
            words[j] = 0;
            for (uint k = 0; k < 8; k++) {
                words[j] |= ((block[j * 8 + k] & 0x00000000000000FFL) << (56 - k * 8));
            }
        }

        for (uint j = 16; j < 80; j++) {
            words[j] = sigma1(words[j -  2]) + words[j -  7]
                     + sigma0(words[j - 15]) + words[j - 16];
        }

        ulong a = H[0];
        ulong b = H[1];
        ulong c = H[2];
        ulong d = H[3];
        ulong e = H[4];
        ulong f = H[5];
        ulong g = H[6];
        ulong h = H[7];

        for (uint j = 0; j < 80; j++) {

            ulong T1 = h + ch(e, f, g) + sum1(e) + words[j] + K[j];
            ulong T2 = sum0(a) + maj(a, b, c);

            h = g;
            g = f;
            f = e;
            e = d + T1;
            d = c;
            c = b;
            b = a;
            a = T1 + T2;

        }

        H[0] += a;
        H[1] += b;
        H[2] += c;
        H[3] += d;
        H[4] += e;
        H[5] += f;
        H[6] += g;
        H[7] += h;
    }

    uint digestOffset = id*64;

    for (uint i = 0; i < 8; i++) {
        uint index = 8*i + digestOffset;
        digest[index+0] = (short)(H[i] >> (56 - 8 * 0)) & 0xffL;
        digest[index+1] = (short)(H[i] >> (56 - 8 * 1)) & 0xffL;
        digest[index+2] = (short)(H[i] >> (56 - 8 * 2)) & 0xffL;
        digest[index+3] = (short)(H[i] >> (56 - 8 * 3)) & 0xffL;
        digest[index+4] = (short)(H[i] >> (56 - 8 * 4)) & 0xffL;
        digest[index+5] = (short)(H[i] >> (56 - 8 * 5)) & 0xffL;
        digest[index+6] = (short)(H[i] >> (56 - 8 * 6)) & 0xffL;
        digest[index+7] = (short)(H[i] >> (56 - 8 * 7)) & 0xffL;
    }

}
