package dev.shiro8613.fixio.nativeapi.compression;

import dev.shiro8613.fixio.nativeapi.io.DirectBufferOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

public class NativeZlibOutputStream extends OutputStream {
    private final OutputStream out;
    private final DirectBufferOutputStream buffer = new DirectBufferOutputStream(16 * 1024);
    private boolean closed = false;

    public NativeZlibOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override public void write(int b) {
        buffer.write(b);
    }
    @Override public void write(byte[] b, int off, int len) {
        buffer.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;

            ByteBuffer buf = buffer.getBuffer();
            if (buf != null && buf.remaining() > 0) {
                ByteBuffer compressedBuffer = CompressorHolder.get().zlibCompressSmartDirect(
                    MemoryUtil.memAddress(buf), 0, buf.remaining()
                );
                buffer.close();

                if (compressedBuffer == null) {
                    throw new IOException("Failed to compress ZLIB buffer via libdeflate (Smart Native)");
                }

                byte[] outputBytes = new byte[compressedBuffer.remaining()];
                compressedBuffer.get(outputBytes);
                out.write(outputBytes);

                NativeCompressor.freeCBuffer(compressedBuffer);
            }

            out.close();
        }
    }
}