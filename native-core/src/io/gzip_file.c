#define _CRT_SECURE_NO_WARNINGS 

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <libdeflate.h>
#include "deflate_utils.h"

JNIEXPORT jobject JNICALL
Java_dev_shiro8613_fixio_nativeapi_io_NativeGzipFile_readAndDecompressFileNative(
    JNIEnv *env, jclass clazz,
    jlong ctx_ptr, jstring path_str, jint estimated_uncompressed_size
) {
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr;
    if (!ctx || !ctx->decompressor || !path_str) return NULL;

    const char *path = (*env)->GetStringUTFChars(env, path_str, NULL);
    if (!path) return NULL;

    FILE *f = fopen(path, "rb");
    (*env)->ReleaseStringUTFChars(env, path_str, path);
    if (!f) return NULL;

    
    fseek(f, 0, SEEK_END);
    long file_size = ftell(f);
    fseek(f, 0, SEEK_SET);

    if (file_size <= 0) {
        fclose(f);
        return NULL;
    }

    
    uint8_t *compressed_buf = (uint8_t*)malloc(file_size);
    if (!compressed_buf) {
        fclose(f);
        return NULL;
    }

    size_t read_bytes = fread(compressed_buf, 1, file_size, f);
    fclose(f);

    if (read_bytes != (size_t)file_size) {
        free(compressed_buf);
        return NULL;
    }

    
    size_t dst_capacity = (estimated_uncompressed_size > 0) 
        ? (size_t)estimated_uncompressed_size 
        : ((size_t)file_size * 4);

    uint8_t *decompressed_buf = (uint8_t*)malloc(dst_capacity);
    if (!decompressed_buf) {
        free(compressed_buf);
        return NULL;
    }

    size_t actual_out_bytes = 0;
    enum libdeflate_result res;

    
    while (1) {
        res = libdeflate_gzip_decompress(
            ctx->decompressor, compressed_buf, (size_t)file_size,
            decompressed_buf, dst_capacity, &actual_out_bytes
        );

        if (res == LIBDEFLATE_INSUFFICIENT_SPACE) {
            dst_capacity *= 2; 
            uint8_t *new_buf = (uint8_t*)realloc(decompressed_buf, dst_capacity);
            if (!new_buf) {
                free(decompressed_buf);
                free(compressed_buf);
                return NULL;
            }
            decompressed_buf = new_buf;
        } else {
            break; 
        }
    }

    free(compressed_buf); 

    if (res != LIBDEFLATE_SUCCESS) {
        free(decompressed_buf);
        return NULL;
    }
    
    return (*env)->NewDirectByteBuffer(env, decompressed_buf, (jlong)actual_out_bytes);
}

JNIEXPORT jboolean JNICALL
Java_dev_shiro8613_fixio_nativeapi_io_NativeGzipFile_compressAndWriteFileNative(
    JNIEnv *env, jclass clazz,
    jlong ctx_ptr, jstring path_str, jlong src_address, jint src_len, jint compression_level
) {
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr;

    if (!ctx || !ctx->compressor || !path_str || src_len <= 0) return JNI_FALSE;

    uint8_t *src_buf = (uint8_t*)src_address;
    if (!src_buf) return JNI_FALSE;

    size_t max_dst_size = libdeflate_gzip_compress_bound(ctx->compressor, (size_t)src_len);
    uint8_t *compressed_buf = (uint8_t*)malloc(max_dst_size);
    if (!compressed_buf) {
        return JNI_FALSE;
    }

    size_t actual_compressed_size = libdeflate_gzip_compress(
        ctx->compressor, src_buf, (size_t)src_len, compressed_buf, max_dst_size
    );

    if (actual_compressed_size == 0) {
        free(compressed_buf);
        return JNI_FALSE;
    }

    const char *path = (*env)->GetStringUTFChars(env, path_str, NULL);
    if (!path) {
        free(compressed_buf);
        return JNI_FALSE;
    }

    FILE *f = fopen(path, "wb");
    (*env)->ReleaseStringUTFChars(env, path_str, path);

    if (!f) {
        free(compressed_buf);
        return JNI_FALSE;
    }

    size_t written = fwrite(compressed_buf, 1, actual_compressed_size, f);
    fclose(f);
    free(compressed_buf);

    return (written == actual_compressed_size) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_dev_shiro8613_fixio_nativeapi_io_NativeGzipFile_freeNativeBuffer(
    JNIEnv *env, jclass clazz, jobject direct_buffer
) {
    if (!direct_buffer) return;
    uint8_t* address = (uint8_t*)(*env)->GetDirectBufferAddress(env, direct_buffer);
    if (address) {
        free(address);
    }
}