package dev.shiro8613.fixio.nativeapi.compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class NativeGzipOutputStream extends OutputStream {

    private final OutputStream out;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16 * 1024);
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

            byte[] rawBytes = buffer.toByteArray();
            if (rawBytes.length > 0) {
                ByteBuffer directInput = ByteBuffer.allocateDirect(rawBytes.length);
                directInput.put(rawBytes);
                directInput.flip();

                ByteBuffer directOutput = ByteBuffer.allocateDirect(rawBytes.length + 512);

                int compressedSize = CompressorHolder.get().gzipCompressBuffer(
                    directInput, 0, rawBytes.length,
                    directOutput, 0, directOutput.capacity()
                );

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