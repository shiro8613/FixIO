package dev.shiro8613.fixio.nativeapi.compression;

import dev.shiro8613.fixio.nativeapi.io.DirectBufferInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class NativeZlibInputStream extends InputStream {

    private final ByteBuffer decompressedBuffer;
    private final DirectBufferInputStream delegateStream;
    private boolean closed = false;

    public NativeZlibInputStream(InputStream in) throws IOException {
        byte[] compressedBytes = in.readAllBytes();
        if (compressedBytes.length == 0) {
            throw new IOException("Empty ZLIB stream");
        }

        ByteBuffer srcBuffer = ByteBuffer.allocateDirect(compressedBytes.length);
        srcBuffer.put(compressedBytes);
        srcBuffer.flip();

        this.decompressedBuffer = CompressorHolder.get().zlibDecompressSmartBuffer(
            srcBuffer, 0, compressedBytes.length
        );

        if (this.decompressedBuffer == null) {
            throw new IOException("Failed to decompress ZLIB buffer via libdeflate (Smart Native)");
        }

        this.delegateStream = new DirectBufferInputStream(this.decompressedBuffer);
    }


    @Override public int read() {
        return delegateStream.read();
    }

    @Override public int read(byte[] b, int off, int len) {
        return delegateStream.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            delegateStream.close();
            NativeCompressor.freeCBuffer(decompressedBuffer);
        }
    }
}