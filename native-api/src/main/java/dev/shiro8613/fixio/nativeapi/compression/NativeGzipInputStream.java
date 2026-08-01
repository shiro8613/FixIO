package dev.shiro8613.fixio.nativeapi.compression;

import dev.shiro8613.fixio.nativeapi.io.DirectBufferInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class NativeGzipInputStream extends InputStream {

    private final DirectBufferInputStream decompressedStream;

    public NativeGzipInputStream(InputStream in) throws IOException {
        byte[] compressedBytes = in.readAllBytes();

        ByteBuffer decompressedBuffer = CompressorHolder.get().gzipDecompressAll(compressedBytes);

        if (decompressedBuffer == null) {
            throw new IOException("Failed to decompress GZIP stream via native libdeflate");
        }

        this.decompressedStream = new DirectBufferInputStream(decompressedBuffer);
    }

    @Override
    public int read() {
        return decompressedStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) {
        return decompressedStream.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        decompressedStream.close();
    }
}