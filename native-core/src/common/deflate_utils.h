#pragma once

typedef struct {
    struct libdeflate_decompressor* decompressor;
    struct libdeflate_compressor* compressor;
    int compression_level;
} NativeCompressorCtx;