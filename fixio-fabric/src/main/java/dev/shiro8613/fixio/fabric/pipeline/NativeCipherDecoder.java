package dev.shiro8613.fixio.fabric.pipeline;

import dev.shiro8613.fixio.nativeapi.crypto.NettyNativeCipher;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class NativeCipherDecoder extends ByteToMessageDecoder {

    private final NettyNativeCipher cipher;

    public NativeCipherDecoder(byte[] secret) {
        this.cipher = new NettyNativeCipher(false, secret);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readable = in.readableBytes();
        if (readable == 0) return;

        ByteBuf decrypted = ctx.alloc().directBuffer(readable, readable);
        try {
            this.cipher.process(in, decrypted);
            out.add(decrypted);
        } catch (Exception e) {
            decrypted.release();
            throw e;
        }
    }

    @Override
    public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
        this.cipher.close();
        super.handlerRemoved0(ctx);
    }
}