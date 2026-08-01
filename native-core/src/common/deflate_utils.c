#include <jni.h>
#include <libdeflate.h>
#include <stdlib.h>
#include "deflate_utils.h"

JNIEXPORT jlong JNICALL
Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressorCtx_createContext(
    JNIEnv *env, jclass clazz, jint level
) {
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)malloc(sizeof(NativeCompressorCtx));
    if (!ctx) return 0;

    ctx->compression_level = level;
    ctx->compressor = libdeflate_alloc_compressor(level);
    ctx->decompressor = libdeflate_alloc_decompressor();

    if (!ctx->compressor || !ctx->decompressor) {
        if (ctx->compressor) libdeflate_free_compressor(ctx->compressor);
        if (ctx->decompressor) libdeflate_free_decompressor(ctx->decompressor);
        free(ctx);
        return 0;
    }

    return (jlong)(uintptr_t)ctx;
}

JNIEXPORT void JNICALL
Java_dev_shiro8613_fixio_nativeapi_compression_NativeCompressorCtx_freeContext(
    JNIEnv *env, jclass clazz, jlong ctx_ptr
) {
    NativeCompressorCtx* ctx = (NativeCompressorCtx*)(uintptr_t)ctx_ptr;
    if (!ctx) return;

    if (ctx->compressor) libdeflate_free_compressor(ctx->compressor);
    if (ctx->decompressor) libdeflate_free_decompressor(ctx->decompressor);
    free(ctx);
}