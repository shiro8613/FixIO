package dev.shiro8613.fixio.nativeapi.compression;

public class CompressorHolder {
    private static final ThreadLocal<NativeCompressor> COMPRESSOR_HOLDER =
        ThreadLocal.withInitial(() -> new NativeCompressor(6));

    public static NativeCompressor get() {
        return COMPRESSOR_HOLDER.get();
    }
}
