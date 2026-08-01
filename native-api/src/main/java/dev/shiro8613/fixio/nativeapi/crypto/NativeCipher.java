package dev.shiro8613.fixio.nativeapi.crypto;

import dev.shiro8613.fixio.nativeapi.utils.NativeUtils;
import java.nio.ByteBuffer;

public class NativeCipher implements AutoCloseable {

    static {
        NativeUtils.ensureLoaded();
    }

    private final long ctxPtr;
    private final boolean isEncrypt;
    private boolean closed = false;

    public NativeCipher(boolean isEncrypt, byte[] secret) {
        if (secret == null || secret.length != 16) {
            throw new IllegalArgumentException("Secret key must be exactly 16 bytes.");
        }
        this.isEncrypt = isEncrypt;
        this.ctxPtr = initContext(isEncrypt, secret);
        if (this.ctxPtr == 0) {
            throw new RuntimeException("Failed to initialize OpenSSL cipher context.");
        }
    }

    public int processDirect(long srcAddr, long dstAddr, int len) {
        if (closed) {
            throw new IllegalStateException("Cipher session is already closed.");
        }

        return processDirect(this.ctxPtr, srcAddr, dstAddr, len, this.isEncrypt);
    }

    public int processByteBuffer(ByteBuffer src, ByteBuffer dst, int len) {
        if (closed) {
            throw new IllegalStateException("Cipher session is already closed.");
        }
        return processByteBuffer(this.ctxPtr, src, dst, len, this.isEncrypt);
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            freeContext(this.ctxPtr);
            closed = true;
        }
    }

    private static native long initContext(boolean isEncrypt, byte[] secret);
    private static native int processDirect(long ctxPtr, long srcAddr, long dstAddr, int len, boolean isEncrypt);
    private static native int processByteBuffer(long ctxPtr, ByteBuffer srcBuf, ByteBuffer dstBuf, int len, boolean isEncrypt);
    private static native void freeContext(long ctxPtr);
}
