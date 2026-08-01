package dev.shiro8613.fixio.nativeapi.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NativeCipherTest {

    // Minecraft の暗号化と同じ 16 バイトの共有シークレット
    private static final byte[] SECRET = "1234567890123456".getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("Direct ByteBuffer を使った暗号化・復号のラウンドトリップテスト")
    void testEncryptAndDecryptRoundTrip() {
        String originalText = "Hello, OpenSSL Native AES-128-CFB8!";
        byte[] originalBytes = originalText.getBytes(StandardCharsets.UTF_8);

        // Direct Buffer を確保
        ByteBuffer src = ByteBuffer.allocateDirect(originalBytes.length);
        ByteBuffer encrypted = ByteBuffer.allocateDirect(originalBytes.length);
        ByteBuffer decrypted = ByteBuffer.allocateDirect(originalBytes.length);

        src.put(originalBytes);

        try (NativeCipher encCipher = new NativeCipher(true, SECRET);
            NativeCipher decCipher = new NativeCipher(false, SECRET)) {

            // 1. 暗号化
            int encResult = encCipher.processByteBuffer(src, encrypted, originalBytes.length);
            assertEquals(originalBytes.length, encResult);

            // 暗号化されたデータが平文と異なることを検証
            byte[] encBytes = new byte[originalBytes.length];
            encrypted.get(encBytes);
            assertNotEquals(originalText, new String(encBytes, StandardCharsets.UTF_8));

            // 2. 復号
            int decResult = decCipher.processByteBuffer(encrypted, decrypted, originalBytes.length);
            assertEquals(originalBytes.length, decResult);

            // 3. 検証: 元のテキストに正しく戻っているか
            byte[] decBytes = new byte[originalBytes.length];
            decrypted.get(decBytes);
            assertEquals(originalText, new String(decBytes, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("In-Place (同一メモリアドレス) での暗号化・復号テスト")
    void testInPlaceEncryptionAndDecryption() {
        byte[] randomData = new byte[1024];
        new Random().nextBytes(randomData);

        ByteBuffer buf = ByteBuffer.allocateDirect(randomData.length);
        buf.clear();
        buf.put(randomData);

        try (NativeCipher encCipher = new NativeCipher(true, SECRET);
            NativeCipher decCipher = new NativeCipher(false, SECRET)) {

            // 1. その場で暗号化 (srcAddr == dstAddr)
            int encResult = encCipher.processByteBuffer(buf, buf, randomData.length);
            assertEquals(randomData.length, encResult);

            buf.position(0);

            // 2. その場で復号 (srcAddr == dstAddr)
            int decResult = decCipher.processByteBuffer(buf, buf, randomData.length);
            assertEquals(randomData.length, decResult);

            // 3. 検証: 元のデータと一致するか
            byte[] resultBytes = new byte[randomData.length];
            buf.get(resultBytes);
            assertArrayEquals(randomData, resultBytes);
        }
    }

    @Test
    @DisplayName("CFB8 ストリーム暗号の連続処理テスト")
    void testStreamContinuity() {
        String part1 = "First Chunk Data | ";
        String part2 = "Second Chunk Data";

        byte[] b1 = part1.getBytes(StandardCharsets.UTF_8);
        byte[] b2 = part2.getBytes(StandardCharsets.UTF_8);

        ByteBuffer in1 = ByteBuffer.allocateDirect(b1.length).put(b1);
        ByteBuffer in2 = ByteBuffer.allocateDirect(b2.length).put(b2);

        ByteBuffer enc1 = ByteBuffer.allocateDirect(b1.length);
        ByteBuffer enc2 = ByteBuffer.allocateDirect(b2.length);

        ByteBuffer dec1 = ByteBuffer.allocateDirect(b1.length);
        ByteBuffer dec2 = ByteBuffer.allocateDirect(b2.length);

        try (NativeCipher encCipher = new NativeCipher(true, SECRET);
            NativeCipher decCipher = new NativeCipher(false, SECRET)) {

            // 送信側: 連続暗号化
            encCipher.processByteBuffer(in1, enc1, b1.length);
            encCipher.processByteBuffer(in2, enc2, b2.length);

            // 受信側: 順序通りに復号
            decCipher.processByteBuffer(enc1, dec1, b1.length);
            decCipher.processByteBuffer(enc2, dec2, b2.length);

            // 検証
            byte[] out1 = new byte[b1.length];
            dec1.get(out1);
            assertEquals(part1, new String(out1, StandardCharsets.UTF_8));

            byte[] out2 = new byte[b2.length];
            dec2.get(out2);
            assertEquals(part2, new String(out2, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("不正なキー長 (16バイト以外) で IllegalArgumentException が発生するか")
    void testInvalidKeySize() {
        byte[] invalidSecret = "short_key".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new NativeCipher(true, invalidSecret));
    }

    @Test
    @DisplayName("close 後の呼び出しで IllegalStateException が発生するか")
    void testClosedState() {
        NativeCipher cipher = new NativeCipher(true, SECRET);
        cipher.close();

        assertThrows(IllegalStateException.class, () -> cipher.processDirect(1000, 1000, 10));
    }
}