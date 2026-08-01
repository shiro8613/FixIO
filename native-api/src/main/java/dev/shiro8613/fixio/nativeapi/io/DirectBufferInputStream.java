package dev.shiro8613.fixio.nativeapi.io;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class DirectBufferInputStream extends InputStream {

    private final ByteBuffer buffer;

    public DirectBufferInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() {
        if (!buffer.hasRemaining()) {
            return -1;
        }
        return buffer.get() & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
        if (len == 0) return 0;

        if (!buffer.hasRemaining()) {
            return -1;
        }

        int bytesToRead = Math.min(len, buffer.remaining());
        buffer.get(b, off, bytesToRead);
        return bytesToRead;
    }

    @Override
    public int available() {
        return buffer.remaining();
    }
}