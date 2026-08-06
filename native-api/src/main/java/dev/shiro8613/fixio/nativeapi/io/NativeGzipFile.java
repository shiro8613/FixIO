package dev.shiro8613.fixio.nativeapi.io;

import dev.shiro8613.fixio.nativeapi.compression.CompressorHolder;
import dev.shiro8613.fixio.nativeapi.utils.NativeUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.lwjgl.system.MemoryUtil;

public class NativeGzipFile implements AutoCloseable {

    static {
        NativeUtils.ensureLoaded();
    }

    private final ByteBuffer buffer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public NativeGzipFile(Path path) throws IOException {
        String absolutePath = path.toAbsolutePath().toString();
        ByteBuffer unknownBuffer = readAndDecompressFileNative(CompressorHolder.get().ptr(), absolutePath, 0);
        if (unknownBuffer == null) {
            throw new IOException("Failed to read/decompress native gzip file: " + absolutePath);
        }

        buffer = unknownBuffer;
    }

    public ByteBuffer getBuffer() {
        if (closed.get()) {
            throw new IllegalStateException("NativeGzipBuffer has already been closed!");
        }

        return buffer;
    }


    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            freeNativeBuffer(buffer);
        }
    }

    public static void writeCompressed(Path path, ByteBuffer rawData) throws IOException {
        String absolutePath = path.toAbsolutePath().toString();
        boolean success = compressAndWriteFileNative(CompressorHolder.get().ptr(), absolutePath,
            MemoryUtil.memAddress(rawData), rawData.remaining(), 6);

        if (!success) {
            throw new IOException("Failed to compress and write native gzip file: " + absolutePath);
        }
    }

    private static native ByteBuffer readAndDecompressFileNative(long ctxPtr, String filePath, int estimatedUncompressedSize);
    private static native boolean compressAndWriteFileNative(long ctxPtr, String filePath, long srcAddress, int srcLen, int compressionLevel);
    private static native void freeNativeBuffer(ByteBuffer directBuffer);
}