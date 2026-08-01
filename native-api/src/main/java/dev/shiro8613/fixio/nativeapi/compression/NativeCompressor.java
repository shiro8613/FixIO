package dev.shiro8613.fixio.nativeapi.compression;

import dev.shiro8613.fixio.nativeapi.utils.NativeUtils;
import java.nio.ByteBuffer;

public class NativeCompressor extends NativeCompressorCtx {

    static {
        NativeUtils.ensureLoaded();
    }


    public NativeCompressor(int level) {
        super(level);
    }

    public int zlibCompressDirect(long srcAddress, int srcLen, long dstAddress, int dstCapacity) {
        checkClosed();
        return zlibCompressDirect(ctxPtr, srcAddress, srcLen, dstAddress, dstCapacity);
    }

    public int zlibDecompressDirect(long srcAddress, int srcLen, long dstAddress, int dstCapacity) {
        checkClosed();
        return zlibDecompressDirect(ctxPtr, srcAddress, srcLen, dstAddress, dstCapacity);
    }

    public int zlibCompressBuffer(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity) {
        checkClosed();
        if (!src.isDirect() || !dst.isDirect()) {
            throw new IllegalArgumentException("Buffers must be direct ByteBuffer");
        }
        return zlibCompressBuffer(ctxPtr, src, srcOff, srcLen, dst, dstOff, dstCapacity);
    }

    public int zlibDecompressBuffer(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity) {
        checkClosed();
        if (!src.isDirect() || !dst.isDirect()) {
            throw new IllegalArgumentException("Buffers must be direct ByteBuffer");
        }
        return zlibDecompressBuffer(ctxPtr, src, srcOff, srcLen, dst, dstOff, dstCapacity);
    }

    public ByteBuffer zlibCompressSmartBuffer(ByteBuffer src, int srcOff, int srcLen) {
        checkClosed();
        if (!src.isDirect()) {
            throw new IllegalArgumentException("Buffers must be direct ByteBuffer");
        }
        return zlibCompressSmartBuffer(ctxPtr, src, srcOff, srcLen);
    }

    public ByteBuffer zlibDecompressSmartBuffer(ByteBuffer src, int srcOff, int srcLen) {
        checkClosed();
        if (!src.isDirect()) {
            throw new IllegalArgumentException("Buffers must be direct ByteBuffer");
        }
        return zlibDecompressSmartBuffer(ctxPtr, src, srcOff, srcLen);
    }


    public int gzipCompressDirect(long srcAddress, int srcLen, long dstAddress, int dstCapacity) {
        checkClosed();
        return gzipCompressDirect(ctxPtr, srcAddress, srcLen, dstAddress, dstCapacity);
    }

    public int gzipDecompressDirect(long srcAddress, int srcLen, long dstAddress, int dstCapacity) {
        checkClosed();
        return gzipDecompressDirect(ctxPtr, srcAddress, srcLen, dstAddress, dstCapacity);
    }

    public int gzipCompressBuffer(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity) {
        checkClosed();
        if (!src.isDirect() || !dst.isDirect()) {
            throw new IllegalArgumentException("Buffers must be direct ByteBuffer");
        }
        return gzipCompressBuffer(ctxPtr, src, srcOff, srcLen, dst, dstOff, dstCapacity);
    }

    public int gzipDecompressBuffer(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity) {
        checkClosed();
        if (!src.isDirect() || !dst.isDirect()) {
            throw new IllegalArgumentException("Buffers must be direct ByteBuffer");
        }
        return gzipDecompressBuffer(ctxPtr, src, srcOff, srcLen, dst, dstOff, dstCapacity);
    }

    public ByteBuffer gzipDecompressAll(byte[] src) {
        checkClosed();
        return gzipDecompressAll(ctxPtr, src, src.length);
    }

    private void checkClosed() {
        if (ctxPtr == 0) {
            throw new IllegalStateException("Compressor is closed");
        }
    }

    // --- Native JNI Methods ---

    private static native int zlibCompressDirect(long ctx, long srcAddress, int srcLen, long dstAddress, int dstCapacity);
    private static native int zlibDecompressDirect(long ctx, long srcAddress, int srcLen, long dstAddress, int dstCapacity);
    private static native int zlibCompressBuffer(long ctx, ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity);
    private static native int zlibDecompressBuffer(long ctx, ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity);
    private static native ByteBuffer zlibDecompressSmartBuffer(long ctxPtr, ByteBuffer src, int srcOff, int srcLen);
    private static native ByteBuffer zlibCompressSmartBuffer(long ctxPtr, ByteBuffer src, int srcOff, int srcLen);

    private static native int gzipCompressDirect(long ctx, long srcAddress, int srcLen, long dstAddress, int dstCapacity);
    private static native int gzipDecompressDirect(long ctx, long srcAddress, int srcLen, long dstAddress, int dstCapacity);
    private static native int gzipCompressBuffer(long ctx, ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity);
    private static native int gzipDecompressBuffer(long ctx, ByteBuffer src, int srcOff, int srcLen, ByteBuffer dst, int dstOff, int dstCapacity);
    private static native ByteBuffer gzipDecompressAll(long ctx, byte[] src, int length);

    public static native void freeCBuffer(ByteBuffer buf);
}