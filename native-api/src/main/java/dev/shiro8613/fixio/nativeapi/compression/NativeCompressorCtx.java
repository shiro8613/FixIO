package dev.shiro8613.fixio.nativeapi.compression;

import dev.shiro8613.fixio.nativeapi.utils.NativeUtils;

public class NativeCompressorCtx implements AutoCloseable{

    static {
        NativeUtils.ensureLoaded();
    }

    long ctxPtr;

    public NativeCompressorCtx(int level) {
        this.ctxPtr = createContext(level);
        if (this.ctxPtr == 0) {
            throw new RuntimeException("Failed to create libdeflate compressor context");
        }
    }

    public long ptr() {
        return ctxPtr;
    }

    @Override
    public void close() {
        if (ctxPtr != 0) {
            freeContext(ctxPtr);
            ctxPtr = 0;
        }
    }

    private static native long createContext(int level);
    private static native void freeContext(long ctx);
}
