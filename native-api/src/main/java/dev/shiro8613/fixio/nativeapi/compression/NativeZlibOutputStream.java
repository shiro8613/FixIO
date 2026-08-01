package dev.shiro8613.fixio.nativeapi.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class NativeZlibOutputStream extends OutputStream {

    private final OutputStream out;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16384);
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

            byte[] rawBytes = buffer.toByteArray();
            if (rawBytes.length > 0) {
                ByteBuffer srcBuffer = ByteBuffer.allocateDirect(rawBytes.length);
                srcBuffer.put(rawBytes);
                srcBuffer.flip();

                ByteBuffer compressedBuffer = CompressorHolder.get().zlibCompressSmartBuffer(
                    srcBuffer, 0, rawBytes.length
                );

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