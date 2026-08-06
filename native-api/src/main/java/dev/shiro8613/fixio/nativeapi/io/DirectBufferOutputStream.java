package dev.shiro8613.fixio.nativeapi.io;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

public final class DirectBufferOutputStream extends OutputStream {
    private ByteBuffer buffer;
    private int capa;

    public DirectBufferOutputStream(int size) {
        this.buffer = MemoryUtil.memAlloc(size);
        this.capa = size;
    }

    public DirectBufferOutputStream(ByteBuffer buffer) {
        this.buffer = buffer;
        this.capa = buffer.capacity();
    }

    private void ensureCapacity(int minCapacity) {
        if (capa < minCapacity) {
            capa = Math.max(capa * 2, minCapacity);
            int oldPos = buffer.position();
            buffer = MemoryUtil.memRealloc(buffer, capa);
            buffer.position(oldPos);
        }
    }

    @Override
    public void write(int b) {
        ensureCapacity(buffer.position() + 1);
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        ensureCapacity(buffer.position() + len);
        buffer.put(b, off, len);
    }

    public ByteBuffer getBuffer() {
        buffer.flip();
        return buffer;
    }

    @Override
    public void close() {
        if (buffer != null) {
            MemoryUtil.memFree(buffer);
            buffer = null;
        }
    }
}
