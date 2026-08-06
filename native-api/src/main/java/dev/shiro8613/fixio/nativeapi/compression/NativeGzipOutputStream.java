package dev.shiro8613.fixio.nativeapi.compression;

import dev.shiro8613.fixio.nativeapi.io.DirectBufferOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

public class NativeGzipOutputStream extends OutputStream {

    private final OutputStream out;
    private final DirectBufferOutputStream buffer = new DirectBufferOutputStream(16 * 1024);
    private boolean closed = false;

    public NativeGzipOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;

            ByteBuffer buf = buffer.getBuffer();
            if (buf != null && buf.remaining() > 0) {
                ByteBuffer directOutput = MemoryUtil.memAlloc(buf.remaining() + 512);

                int compressedSize = CompressorHolder.get().gzipCompressDirect(
                    MemoryUtil.memAddress(buf), buf.remaining(),
                    MemoryUtil.memAddress(directOutput), directOutput.capacity()
                );
                buffer.close();

                if (compressedSize < 0) {
                    throw new IOException("Failed to compress GZIP stream via native libdeflate");
                }

                byte[] compressedBytes = new byte[compressedSize];
                directOutput.get(compressedBytes);
                out.write(compressedBytes);
            }

            out.close();
        }
    }
}