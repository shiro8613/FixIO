package dev.shiro8613.fixio.nativeapi.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class NativeCompressorTest {

    @Test
    @DisplayName("コンテキストの作成と解放がクラッシュせずに実行できるか")
    void testLifecycle() {
        assertDoesNotThrow(() -> {
            try (NativeCompressor compressor = new NativeCompressor(6)) {
                assertNotNull(compressor);
            }
        });
    }

    @Test
    @DisplayName("zlibによる圧縮、解凍テスト")
    void testZlibCompressAndDecompress() {
        String testMessage = "Hello Minecraft Native World! This is a test string for libdeflate compression. "
            + "Hello Minecraft Native World! This is a test string for libdeflate compression.";
        byte[] inputBytes = testMessage.getBytes(StandardCharsets.UTF_8);

        ByteBuffer srcBuf = ByteBuffer.allocateDirect(inputBytes.length);
        ByteBuffer compressedBuf = ByteBuffer.allocateDirect(inputBytes.length * 2);
        ByteBuffer decompressedBuf = ByteBuffer.allocateDirect(inputBytes.length * 2);

        // 入力データを書き込み
        srcBuf.put(inputBytes);

        try (NativeCompressor compressor = new NativeCompressor(6)) {
            // 1. 圧縮 (オフセット0からサイズ分)
            int compressedSize = compressor.zlibCompressBuffer(
                srcBuf, 0, inputBytes.length,
                compressedBuf, 0, compressedBuf.capacity()
            );
            assertTrue(compressedSize > 0, "圧縮に失敗しました。エラーコード: " + compressedSize);

            // 2. 解凍
            int decompressedSize = compressor.zlibDecompressBuffer(
                compressedBuf, 0, compressedSize,
                decompressedBuf, 0, decompressedBuf.capacity()
            );
            assertTrue(decompressedSize > 0, "解凍に失敗しました。エラーコード: " + decompressedSize);
            assertEquals(inputBytes.length, decompressedSize, "解凍後のサイズが元データと一致しません");

            // 3. データ取り出し
            byte[] resultBytes = new byte[decompressedSize];
            decompressedBuf.position(0);
            decompressedBuf.get(resultBytes, 0, decompressedSize);

            assertEquals(testMessage, new String(resultBytes, StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("Javaのgzipとの解凍互換性テスト")
    void testDecompressJavaGzipWithNative() throws Exception {
        String testData = "Hello Minecraft NbtIO! Testing GZIP decompression with libdeflate natively.";
        byte[] originalBytes = testData.getBytes(StandardCharsets.UTF_8);

        // Java標準で GZIP 圧縮
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(originalBytes);
        }
        byte[] gzipCompressedBytes = baos.toByteArray();

        // Direct ByteBuffer に読み込み
        ByteBuffer srcBuffer = ByteBuffer.allocateDirect(gzipCompressedBytes.length);
        srcBuffer.put(gzipCompressedBytes);

        ByteBuffer dstBuffer = ByteBuffer.allocateDirect(originalBytes.length * 2);

        // Native 側で GZIP 解凍
        try (NativeCompressor compressor = new NativeCompressor(6)) {
            int decompressedSize = compressor.gzipDecompressBuffer(
                srcBuffer, 0, gzipCompressedBytes.length,
                dstBuffer, 0, dstBuffer.capacity()
            );

            assertTrue(decompressedSize > 0, "解凍に失敗しました (エラーコード: " + decompressedSize + ")");
            assertEquals(originalBytes.length, decompressedSize);

            byte[] resultBytes = new byte[decompressedSize];
            dstBuffer.get(resultBytes);

            assertArrayEquals(originalBytes, resultBytes, "解凍されたデータが元のデータと一致しません");
        }
    }

    @Test
    @DisplayName("Javaのgzipとの圧縮互換性テスト")
    void testCompressNativeGzipWithJava() throws Exception {
        String testData = "Testing Native GZIP Compression for NbtIO optimization!";
        byte[] originalBytes = testData.getBytes(StandardCharsets.UTF_8);

        ByteBuffer srcBuffer = ByteBuffer.allocateDirect(originalBytes.length);
        srcBuffer.put(originalBytes);

        // GZIP ヘッダー/フッター込みの十分な容量を用意
        ByteBuffer dstBuffer = ByteBuffer.allocateDirect(originalBytes.length + 64);

        // Native 側で GZIP 圧縮
        try (NativeCompressor compressor = new NativeCompressor(6)) {
            int compressedSize = compressor.gzipCompressBuffer(
                srcBuffer, 0, originalBytes.length,
                dstBuffer, 0, dstBuffer.capacity()
            );

            assertTrue(compressedSize > 0,
                "圧縮に失敗しました (エラーコード: " + compressedSize + ")");

            byte[] compressedBytes = new byte[compressedSize];
            dstBuffer.get(compressedBytes);

            // Java標準の GZIPInputStream で解凍して検証
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
            try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                byte[] decompressedBytes = gzis.readAllBytes();
                assertArrayEquals(originalBytes, decompressedBytes,
                    "Java標準のGZIPで解凍できませんでした");
            }
        }
    }

    @Test
    @DisplayName("クローズ後に操作しようとした場合に IllegalStateException が飛ぶか")
    void testClosedState() {
        NativeCompressor compressor = new NativeCompressor(6);
        compressor.close();

        assertThrows(IllegalStateException.class, () ->
            compressor.zlibCompressDirect(1000, 0, 10, 2000, 0, 100)
        );
    }
}