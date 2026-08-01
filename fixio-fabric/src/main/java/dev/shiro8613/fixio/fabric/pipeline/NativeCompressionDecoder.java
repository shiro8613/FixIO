package dev.shiro8613.fixio.fabric.pipeline;

import dev.shiro8613.fixio.nativeapi.compression.NettyNativeCompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;

import java.util.List;
import net.minecraft.network.VarInt;

public class NativeCompressionDecoder extends ByteToMessageDecoder {
    public static final int MAXIMUM_UNCOMPRESSED_LENGTH = 8388608;

    private int threshold;
    private boolean validateDecompressed;

    public NativeCompressionDecoder(final int threshold, final boolean validateDecompressed) {
        this.threshold = threshold;
        this.validateDecompressed = validateDecompressed;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
        if (in.readableBytes() == 0) {
            return;
        }

        int uncompressedLength = VarInt.read(in);

        if (uncompressedLength == 0) {
            out.add(in.readBytes(in.readableBytes()));
        } else {
            if (this.validateDecompressed) {
                if (uncompressedLength < this.threshold) {
                    throw new DecoderException("Badly compressed packet - size of " + uncompressedLength + " is below server threshold of " + this.threshold);
                }

                if (uncompressedLength > MAXIMUM_UNCOMPRESSED_LENGTH) {
                    throw new DecoderException("Badly compressed packet - size of " + uncompressedLength + " is larger than protocol maximum of " + MAXIMUM_UNCOMPRESSED_LENGTH);
                }
            }

            ByteBuf output = ctx.alloc().directBuffer(uncompressedLength, MAXIMUM_UNCOMPRESSED_LENGTH);

            try {
                int actualUncompressedLength = NettyNativeCompressor.decompress(in, output, uncompressedLength);

                if (actualUncompressedLength < 0) {
                    throw new DecoderException("Failed to decompress packet with libdeflate (error code: " + actualUncompressedLength + ")");
                }

                if (actualUncompressedLength != uncompressedLength) {
                    throw new DecoderException(
                        "Badly compressed packet - actual length of uncompressed payload "
                            + actualUncompressedLength
                            + " does not match declared size "
                            + uncompressedLength
                    );
                }

                out.add(output);

            } catch (Exception e) {
                output.release();
                throw e;
            }
        }
    }

    public void setThreshold(final int threshold, final boolean validateDecompressed) {
        this.threshold = threshold;
        this.validateDecompressed = validateDecompressed;
    }
}