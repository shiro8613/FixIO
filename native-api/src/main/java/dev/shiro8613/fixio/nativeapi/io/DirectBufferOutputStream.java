package dev.shiro8613.fixio.nativeapi.io;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class DirectBufferOutputStream extends OutputStream {

    private ByteBuffer buffer;

    public DirectBufferOutputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public void write(int b) {
        ensureCapacity(1);
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ensureCapacity(len);
        buffer.put(b, off, len);
    }

    private void ensureCapacity(int length) {
        if (buffer.remaining() < length) {
            int newCapacity = Math.max(buffer.capacity() * 2, buffer.capacity() + length);
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(newCapacity);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
        }
    }
}