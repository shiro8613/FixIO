package dev.shiro8613.fixio.fabric.pipeline;

import dev.shiro8613.fixio.nativeapi.compression.NettyNativeCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.VarInt;

public class NativeCompressionEncoder extends MessageToByteEncoder<ByteBuf> {
    private int threshold;

    public NativeCompressionEncoder(int threshold) {
        this.threshold = threshold;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf uncompressed, ByteBuf out) {
        int uncompressedLength = uncompressed.readableBytes();

        if (uncompressedLength < this.threshold) {
            VarInt.write(out, 0);
            out.writeBytes(uncompressed);
        } else {
            VarInt.write(out, uncompressedLength);

            int result = NettyNativeCompressor.compress(uncompressed, out);
            if (result <= 0) {
                throw new RuntimeException("Compression failed: " + result);
            }
        }
    }

    public void setThreshold(final int threshold) {
        this.threshold = threshold;
    }

}
