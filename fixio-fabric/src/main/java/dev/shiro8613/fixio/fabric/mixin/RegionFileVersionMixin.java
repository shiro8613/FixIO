package dev.shiro8613.fixio.fabric.mixin;

import dev.shiro8613.fixio.nativeapi.compression.NativeZlibInputStream;
import dev.shiro8613.fixio.nativeapi.compression.NativeZlibOutputStream;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@Mixin(RegionFileVersion.class)
public class RegionFileVersionMixin {

    @Shadow @Final public static RegionFileVersion VERSION_DEFLATE;

    @Inject(method = "wrap(Ljava/io/InputStream;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true)
    private void onWrapInputStream(InputStream is, CallbackInfoReturnable<InputStream> cir) throws Exception {
        RegionFileVersion self = (RegionFileVersion) (Object) this;

        if (self == VERSION_DEFLATE) {
            cir.setReturnValue(new FastBufferedInputStream(new NativeZlibInputStream(is)));
        }
    }

    @Inject(method = "wrap(Ljava/io/OutputStream;)Ljava/io/OutputStream;", at = @At("HEAD"), cancellable = true)
    private void onWrapOutputStream(OutputStream os, CallbackInfoReturnable<OutputStream> cir) {
        RegionFileVersion self = (RegionFileVersion) (Object) this;

        if (self == VERSION_DEFLATE) {
            cir.setReturnValue(new BufferedOutputStream(new NativeZlibOutputStream(os)));
        }
    }
}