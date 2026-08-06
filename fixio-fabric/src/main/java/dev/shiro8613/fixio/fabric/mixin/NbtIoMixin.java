package dev.shiro8613.fixio.fabric.mixin;

import dev.shiro8613.fixio.nativeapi.compression.NativeGzipInputStream;
import dev.shiro8613.fixio.nativeapi.compression.NativeGzipOutputStream;
import dev.shiro8613.fixio.nativeapi.io.DirectBufferInputStream;
import dev.shiro8613.fixio.nativeapi.io.DirectBufferOutputStream;
import dev.shiro8613.fixio.nativeapi.io.NativeGzipFile;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;

@Mixin(NbtIo.class)
public abstract class NbtIoMixin {

    @Shadow
    private static CompoundTag read(DataInput input, NbtAccounter accounter) throws IOException {
        throw new AssertionError();
    }

    @Shadow
    private static void write(CompoundTag tag, DataOutput output) throws IOException {
        throw new AssertionError();
    }

    @Inject(
        method = "readCompressed(Ljava/nio/file/Path;Lnet/minecraft/nbt/NbtAccounter;)Lnet/minecraft/nbt/CompoundTag;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onReadCompressedPath(Path file, NbtAccounter accounter, CallbackInfoReturnable<CompoundTag> cir) throws IOException {
        try (NativeGzipFile nativeFile = new NativeGzipFile(file)) {
            ByteBuffer buffer = nativeFile.getBuffer();

            try (DataInputStream dis = new DataInputStream(new DirectBufferInputStream(buffer))) {
                cir.setReturnValue(read(dis, accounter));
            }
        }
    }

    @Inject(
        method = "createDecompressorStream",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onCreateDecompressorStream(InputStream in, CallbackInfoReturnable<DataInputStream> cir) throws IOException {
        cir.setReturnValue(new DataInputStream(new NativeGzipInputStream(in)));
    }

    @Inject(
        method = "writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onWriteCompressedPath(CompoundTag tag, Path path, CallbackInfo ci) throws IOException {
        DirectBufferOutputStream dbos = new DirectBufferOutputStream(1024 * 1024);

        try (DataOutputStream dos = new DataOutputStream(dbos)) {
            write(tag, dos);
            NativeGzipFile.writeCompressed(path, dbos.getBuffer());
        }


        ci.cancel();
    }

    @Inject(
        method = "createCompressorStream",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onCreateCompressorStream(OutputStream out, CallbackInfoReturnable<DataOutputStream> cir) throws IOException {
        cir.setReturnValue(new DataOutputStream(new NativeGzipOutputStream(out)));
    }
}