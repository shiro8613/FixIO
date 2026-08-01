package dev.shiro8613.fixio.nativeapi.compression;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public class NettyNativeCompressor {

    public static int compress(ByteBuf src, ByteBuf dst) {
        int uncompressedLength = src.readableBytes();
        if (uncompressedLength == 0) return 0;

        NativeCompressor compressor = CompressorHolder.get();
        int maxCompressedSize = uncompressedLength + 64;
        dst.ensureWritable(maxCompressedSize);

        if (src.hasMemoryAddress() && dst.hasMemoryAddress()) {
            long srcAddr = src.memoryAddress() + src.readerIndex();
            long dstAddr = dst.memoryAddress() + dst.writerIndex();

            int compressedSize = compressor.zlibCompressDirect(
                srcAddr, uncompressedLength,
                dstAddr, dst.writableBytes()
            );

            if (compressedSize > 0) {
                src.skipBytes(uncompressedLength);
                dst.writerIndex(dst.writerIndex() + compressedSize);
            }
            return compressedSize;
        }

        ByteBuffer srcNio = src.nioBuffer(src.readerIndex(), uncompressedLength);
        ByteBuffer dstNio = dst.nioBuffer(dst.writerIndex(), dst.writableBytes());

        int compressedSize = compressor.zlibCompressBuffer(
            srcNio, srcNio.position(), uncompressedLength,
            dstNio, dstNio.position(), dstNio.remaining()
        );

        if (compressedSize > 0) {
            src.skipBytes(uncompressedLength);
            dst.writerIndex(dst.writerIndex() + compressedSize);
        }
        return compressedSize;
    }

    public static int decompress(ByteBuf src, ByteBuf dst, int uncompressedLength) {
        if (src.readableBytes() == 0) return 0;

        NativeCompressor compressor = CompressorHolder.get();
        dst.ensureWritable(uncompressedLength);

        if (src.hasMemoryAddress() && dst.hasMemoryAddress()) {
            long srcAddr = src.memoryAddress() + src.readerIndex();
            long dstAddr = dst.memoryAddress() + dst.writerIndex();

            int decompressedSize = compressor.zlibDecompressDirect(
                srcAddr, src.readableBytes(),
                dstAddr, uncompressedLength
            );

            if (decompressedSize > 0) {
                src.skipBytes(src.readableBytes());
                dst.writerIndex(dst.writerIndex() + decompressedSize);
            }
            return decompressedSize;
        }

        ByteBuffer srcNio = src.nioBuffer(src.readerIndex(), src.readableBytes());
        ByteBuffer dstNio = dst.nioBuffer(dst.writerIndex(), uncompressedLength);

        int decompressedSize = compressor.zlibDecompressBuffer(
            srcNio, srcNio.position(), srcNio.remaining(),
            dstNio, dstNio.position(), uncompressedLength
        );

        if (decompressedSize > 0) {
            src.skipBytes(src.readableBytes());
            dst.writerIndex(dst.writerIndex() + decompressedSize);
        }
        return decompressedSize;
    }
}