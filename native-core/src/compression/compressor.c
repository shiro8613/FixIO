#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <libdeflate.h>
#include "deflate_utils.h"

#define DEFINE_COMPRESS_DIRECT(FUNC_NAME, LIBDEFLATE_FUNC) \
JNIEXPORT jint JNICALL FUNC_NAME( \
    JNIEnv *env, jclass clazz, jlong ctx_ptr, \
    jlong src_address, jint src_len, jlong dst_address, jint dst_capacity \
) { \
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr; \
    if (!ctx || !ctx->compressor) return -1; \
    uint8_t* src = (uint8_t*)(uintptr_t)src_address; \
    uint8_t* dst = (uint8_t*)(uintptr_t)dst_address; \
    if (!src || !dst) return -3; \
    size_t actual_out = LIBDEFLATE_FUNC(ctx->compressor, src, (size_t)src_len, dst, (size_t)dst_capacity); \
    return (actual_out == 0) ? -2 : (jint)actual_out; \
}


#define DEFINE_DECOMPRESS_DIRECT(FUNC_NAME, LIBDEFLATE_FUNC) \
JNIEXPORT jint JNICALL FUNC_NAME( \
    JNIEnv *env, jclass clazz, jlong ctx_ptr, \
    jlong src_address, jint src_len, jlong dst_address, jint dst_capacity \
) { \
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr; \
    if (!ctx || !ctx->decompressor) return -1; \
    uint8_t* src = (uint8_t*)(uintptr_t)src_address; \
    uint8_t* dst = (uint8_t*)(uintptr_t)dst_address; \
    if (!src || !dst) return -3; \
    size_t actual_out = 0; \
    enum libdeflate_result res = LIBDEFLATE_FUNC(ctx->decompressor, src, (size_t)src_len, dst, (size_t)dst_capacity, &actual_out); \
    return (res != LIBDEFLATE_SUCCESS) ? -2 : (jint)actual_out; \
}


#define DEFINE_COMPRESS_BUFFER(FUNC_NAME, LIBDEFLATE_FUNC) \
JNIEXPORT jint JNICALL FUNC_NAME( \
    JNIEnv *env, jclass clazz, jlong ctx_ptr, \
    jobject src_buf, jint src_off, jint src_len, jobject dst_buf, jint dst_off, jint dst_capacity \
) { \
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr; \
    if (!ctx || !ctx->compressor) return -1; \
    uint8_t* src = (uint8_t*)(*env)->GetDirectBufferAddress(env, src_buf); \
    uint8_t* dst = (uint8_t*)(*env)->GetDirectBufferAddress(env, dst_buf); \
    if (!src || !dst) return -3; \
    size_t actual_out = LIBDEFLATE_FUNC(ctx->compressor, src + src_off, (size_t)src_len, dst + dst_off, (size_t)dst_capacity); \
    return (actual_out == 0) ? -2 : (jint)actual_out; \
}


#define DEFINE_DECOMPRESS_BUFFER(FUNC_NAME, LIBDEFLATE_FUNC) \
JNIEXPORT jint JNICALL FUNC_NAME( \
    JNIEnv *env, jclass clazz, jlong ctx_ptr, \
    jobject src_buf, jint src_off, jint src_len, jobject dst_buf, jint dst_off, jint dst_capacity \
) { \
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr; \
    if (!ctx || !ctx->decompressor) return -1; \
    uint8_t* src = (uint8_t*)(*env)->GetDirectBufferAddress(env, src_buf); \
    uint8_t* dst = (uint8_t*)(*env)->GetDirectBufferAddress(env, dst_buf); \
    if (!src || !dst) return -3; \
    size_t actual_out = 0; \
    enum libdeflate_result res = LIBDEFLATE_FUNC(ctx->decompressor, src + src_off, (size_t)src_len, dst + dst_off, (size_t)dst_capacity, &actual_out); \
    return (res != LIBDEFLATE_SUCCESS) ? -2 : (jint)actual_out; \
}

// ==========================================
// スマート解凍（0x78ヘッダー自動探索 + realloc自動拡張）
// ==========================================
#define DEFINE_DECOMPRESS_SMART_BUFFER(FUNC_NAME, LIBDEFLATE_FUNC) \
JNIEXPORT jobject JNICALL FUNC_NAME( \
    JNIEnv *env, jclass clazz, jlong ctx_ptr, \
    jobject src_buf, jint src_off, jint src_len \
) { \
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr; \
    if (!ctx || !ctx->decompressor) return NULL; \
    uint8_t* src = (uint8_t*)(*env)->GetDirectBufferAddress(env, src_buf); \
    if (!src || src_len <= 0) return NULL; \
    \
    uint8_t* src_start = src + src_off; \
    \
    /* 1. ZLIB ヘッダー (0x78) の高速探索 */ \
    int offset = 0; \
    while (offset < src_len && src_start[offset] != 0x78) { \
        offset++; \
    } \
    if (offset >= src_len) offset = 0; \
    \
    uint8_t* actual_src = src_start + offset; \
    size_t actual_src_len = (size_t)(src_len - offset); \
    \
    /* 2. C 側ヒープへ初期バッファ確保 (512KB) */ \
    size_t dst_cap = 512 * 1024; \
    uint8_t* dst = (uint8_t*)malloc(dst_cap); \
    if (!dst) return NULL; \
    \
    /* 3. C 側でのリトライループ (realloc で高速拡張) */ \
    while (1) { \
        size_t actual_out = 0; \
        enum libdeflate_result res = LIBDEFLATE_FUNC(ctx->decompressor, actual_src, actual_src_len, dst, dst_cap, &actual_out); \
        \
        if (res == LIBDEFLATE_SUCCESS) { \
            uint8_t* shrunk_dst = (uint8_t*)realloc(dst, actual_out); \
            if (shrunk_dst) dst = shrunk_dst; \
            return (*env)->NewDirectByteBuffer(env, dst, (jlong)actual_out); \
        } \
        \
        if (res == LIBDEFLATE_INSUFFICIENT_SPACE) { \
            dst_cap *= 2; \
            if (dst_cap > 32 * 1024 * 1024) { /* 最大 32MB */ \
                free(dst); \
                return NULL; \
            } \
            uint8_t* new_dst = (uint8_t*)realloc(dst, dst_cap); \
            if (!new_dst) { \
                free(dst); \
                return NULL; \
            } \
            dst = new_dst; \
            continue; \
        } \
        \
        free(dst); \
        return NULL; \
    } \
}

#define DEFINE_COMPRESS_SMART_BUFFER(FUNC_NAME, LIBDEFLATE_FUNC) \
JNIEXPORT jobject JNICALL FUNC_NAME( \
    JNIEnv *env, jclass clazz, jlong ctx_ptr, \
    jobject src_buf, jint src_off, jint src_len \
) { \
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr; \
    if (!ctx || !ctx->compressor) return NULL; \
    uint8_t* src = (uint8_t*)(*env)->GetDirectBufferAddress(env, src_buf); \
    if (!src || src_len <= 0) return NULL; \
    \
    uint8_t* actual_src = src + src_off; \
    size_t max_dst_size = libdeflate_zlib_compress_bound(ctx->compressor, (size_t)src_len); \
    uint8_t* dst = (uint8_t*)malloc(max_dst_size); \
    if (!dst) return NULL; \
    \
    size_t actual_out = LIBDEFLATE_FUNC(ctx->compressor, actual_src, (size_t)src_len, dst, max_dst_size); \
    if (actual_out == 0) { \
        free(dst); \
        return NULL; \
    } \
    \
    uint8_t* shrunk_dst = (uint8_t*)realloc(dst, actual_out); \
    if (shrunk_dst) dst = shrunk_dst; \
    \
    return (*env)->NewDirectByteBuffer(env, dst, (jlong)actual_out); \
}

DEFINE_DECOMPRESS_SMART_BUFFER(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_zlibDecompressSmartBuffer, libdeflate_zlib_decompress)
DEFINE_COMPRESS_SMART_BUFFER(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_zlibCompressSmartBuffer, libdeflate_zlib_compress)

DEFINE_COMPRESS_DIRECT(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_zlibCompressDirect, libdeflate_zlib_compress)
DEFINE_DECOMPRESS_DIRECT(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_zlibDecompressDirect, libdeflate_zlib_decompress)
DEFINE_COMPRESS_BUFFER(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_zlibCompressBuffer, libdeflate_zlib_compress)
DEFINE_DECOMPRESS_BUFFER(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_zlibDecompressBuffer, libdeflate_zlib_decompress)


DEFINE_COMPRESS_DIRECT(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_gzipCompressDirect, libdeflate_gzip_compress)
DEFINE_DECOMPRESS_DIRECT(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_gzipDecompressDirect, libdeflate_gzip_decompress)
DEFINE_COMPRESS_BUFFER(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_gzipCompressBuffer, libdeflate_gzip_compress)
DEFINE_DECOMPRESS_BUFFER(Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_gzipDecompressBuffer, libdeflate_gzip_decompress)

JNIEXPORT jobject JNICALL
Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_gzipDecompressAll(
    JNIEnv *env, jclass clazz, jlong ctx_ptr, jbyteArray src_array, jint src_len
) {
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr;
    if (!ctx || !ctx->decompressor || !src_array || src_len <= 0) return NULL;

    uint8_t *compressed_buf = (uint8_t*)(*env)->GetPrimitiveArrayCritical(env, src_array, NULL);
    if (!compressed_buf) return NULL;

    size_t dst_capacity = (size_t)src_len * 4;
    uint8_t *decompressed_buf = (uint8_t*)malloc(dst_capacity);
    if (!decompressed_buf) {
        (*env)->ReleasePrimitiveArrayCritical(env, src_array, compressed_buf, JNI_ABORT);
        return NULL;
    }

    size_t actual_out_bytes = 0;
    enum libdeflate_result res;

    while (1) {
        res = libdeflate_gzip_decompress(
            ctx->decompressor, compressed_buf, (size_t)src_len,
            decompressed_buf, dst_capacity, &actual_out_bytes
        );

        if (res == LIBDEFLATE_INSUFFICIENT_SPACE) {
            dst_capacity *= 2;
            uint8_t *new_buf = (uint8_t*)realloc(decompressed_buf, dst_capacity);
            if (!new_buf) {
                free(decompressed_buf);
                (*env)->ReleasePrimitiveArrayCritical(env, src_array, compressed_buf, JNI_ABORT);
                return NULL;
            }
            decompressed_buf = new_buf;
        } else {
            break;
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, src_array, compressed_buf, JNI_ABORT);

    if (res != LIBDEFLATE_SUCCESS) {
        free(decompressed_buf);
        return NULL;
    }

    return (*env)->NewDirectByteBuffer(env, decompressed_buf, (jlong)actual_out_bytes);
}

JNIEXPORT void JNICALL
Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressor_freeCBuffer(
    JNIEnv *env, jclass clazz, jobject direct_buf
) {
    if (!direct_buf) return;
    void* ptr = (*env)->GetDirectBufferAddress(env, direct_buf);
    if (ptr) {
        free(ptr);
    }
}