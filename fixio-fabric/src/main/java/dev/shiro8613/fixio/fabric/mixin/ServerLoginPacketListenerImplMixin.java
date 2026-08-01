package dev.shiro8613.fixio.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.shiro8613.fixio.fabric.NativeEncryptedConnection;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLoginPacketListenerImpl.class)
public class ServerLoginPacketListenerImplMixin {
    @Redirect(
        method = "handleKey",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/Crypt;getCipher(ILjava/security/Key;)Ljavax/crypto/Cipher;"
        )
    )
    private Cipher redirectGetCipher(int opmode, java.security.Key key) {
        return null;
    }

    @Redirect(
        method = "handleKey",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"
        )
    )
    private void redirectSetEncryptionKey(
        Connection connection,
        Cipher decryptCipher,
        Cipher encryptCipher,
        @Local SecretKey secretKey
    ) {
        ((NativeEncryptedConnection) connection).fixIO$setupNativeEncryption(secretKey.getEncoded());
    }
}
