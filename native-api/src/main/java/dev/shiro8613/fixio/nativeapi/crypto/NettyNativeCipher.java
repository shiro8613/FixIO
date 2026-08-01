package dev.shiro8613.fixio.nativeapi.crypto;

import io.netty.buffer.ByteBuf;

public class NettyNativeCipher extends NativeCipher {

    public NettyNativeCipher(boolean isEncrypt, byte[] secret) {
        super(isEncrypt, secret);
    }

    public int process(ByteBuf in, ByteBuf out) {
        int readable = in.readableBytes();
        if (readable == 0) return 0;

        out.ensureWritable(readable);

        if (in.hasMemoryAddress() && out.hasMemoryAddress()) {
            long srcAddr = in.memoryAddress() + in.readerIndex();
            long dstAddr = out.memoryAddress() + out.writerIndex();

            int processed = processDirect(srcAddr, dstAddr, readable);
            if (processed > 0) {
                in.skipBytes(processed);
                out.writerIndex(out.writerIndex() + processed);
            }
            return processed;
        }

        throw new UnsupportedOperationException("Non-direct ByteBuf is not supported for zero-copy crypto");
    }

    public int processInPlace(ByteBuf buf) {
        int readable = buf.readableBytes();
        if (readable == 0) return 0;

        if (buf.hasMemoryAddress()) {
            long addr = buf.memoryAddress() + buf.readerIndex();
            return processDirect(addr, addr, readable);
        }

        throw new UnsupportedOperationException("Non-direct ByteBuf is not supported for zero-copy crypto");
    }
}