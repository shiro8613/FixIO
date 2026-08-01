package dev.shiro8613.fixio.fabric.pipeline;

import dev.shiro8613.fixio.nativeapi.crypto.NettyNativeCipher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NativeCipherEncoder extends MessageToByteEncoder<ByteBuf> {

    private final NettyNativeCipher cipher;

    public NativeCipherEncoder(byte[] secret) {
        this.cipher = new NettyNativeCipher(true, secret);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        cipher.process(in, out);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        this.cipher.close();
        super.handlerRemoved(ctx);
    }
}