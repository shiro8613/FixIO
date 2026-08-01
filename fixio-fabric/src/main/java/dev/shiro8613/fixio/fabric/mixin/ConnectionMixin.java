package dev.shiro8613.fixio.fabric.mixin;

import dev.shiro8613.fixio.fabric.NativeEncryptedConnection;
import dev.shiro8613.fixio.fabric.pipeline.NativeCipherDecoder;
import dev.shiro8613.fixio.fabric.pipeline.NativeCipherEncoder;
import dev.shiro8613.fixio.fabric.pipeline.NativeCompressionDecoder;
import dev.shiro8613.fixio.fabric.pipeline.NativeCompressionEncoder;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin implements NativeEncryptedConnection {

    @Shadow
    private Channel channel;

    @Inject(
        method = "setupCompression",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSetupCompression(final int threshold, final boolean validateDecompressed, CallbackInfo ci) {
        if (threshold >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof NativeCompressionDecoder compressionDecoder) {
                compressionDecoder.setThreshold(threshold, validateDecompressed);
            } else {
                this.channel.pipeline().addAfter("splitter", "decompress", new NativeCompressionDecoder(threshold, validateDecompressed));
            }

            if (this.channel.pipeline().get("compress") instanceof NativeCompressionEncoder compressionEncoder) {
                compressionEncoder.setThreshold(threshold);
            } else {
                this.channel.pipeline().addAfter("prepender", "compress", new NativeCompressionEncoder(threshold));
            }
        } else {
            if (this.channel.pipeline().get("decompress") != null) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") != null) {
                this.channel.pipeline().remove("compress");
            }
        }

        ci.cancel();
    }

    @Override
    public void fixIO$setupNativeEncryption(byte[] secrets) {
        this.channel.pipeline().addBefore("splitter", "decrypt", new NativeCipherDecoder(secrets));
        this.channel.pipeline().addBefore("prepender", "encrypt", new NativeCipherEncoder(secrets));
    }
}
