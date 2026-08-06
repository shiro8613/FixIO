#include "compute.h"
#include <immintrin.h>

static inline __m256i srai_epi64_avx2(__m256i a, int imm) {
    __m256i lshift = _mm256_srli_epi64(a, imm);
    __m256i is_neg = _mm256_cmpgt_epi64(_mm256_setzero_si256(), a);
    __m256i padding = _mm256_slli_epi64(is_neg, 64 - imm);
    return _mm256_or_si256(lshift, padding);
}

int search_sections_avx2(
    const int64_t* keys, int count,
    int x_min, int x_max, int y_min, int y_max, int z_min, int z_max,
    int64_t* result_out
) {
    __m256i v_x_min = _mm256_set1_epi64x(x_min);
    __m256i v_x_max = _mm256_set1_epi64x(x_max);
    __m256i v_y_min = _mm256_set1_epi64x(y_min);
    __m256i v_y_max = _mm256_set1_epi64x(y_max);
    __m256i v_z_min = _mm256_set1_epi64x(z_min);
    __m256i v_z_max = _mm256_set1_epi64x(z_max);

    int hit_count = 0;
    int i = 0;

    for (; i <= count - 4; i += 4) {
        __m256i nodes = _mm256_loadu_si256((const __m256i*)(keys + i));

        __m256i vx = srai_epi64_avx2(_mm256_slli_epi64(nodes, 0), 42);
        __m256i vy = srai_epi64_avx2(_mm256_slli_epi64(nodes, 44), 44);
        __m256i vz = srai_epi64_avx2(_mm256_slli_epi64(nodes, 22), 42);

        __m256i x_ge = _mm256_cmpgt_epi64(vx, _mm256_sub_epi64(v_x_min, _mm256_set1_epi64x(1)));
        __m256i x_le = _mm256_cmpgt_epi64(v_x_max, _mm256_sub_epi64(vx, _mm256_set1_epi64x(1)));
        __m256i x_valid = _mm256_and_si256(x_ge, x_le);

        __m256i y_ge = _mm256_cmpgt_epi64(vy, _mm256_sub_epi64(v_y_min, _mm256_set1_epi64x(1)));
        __m256i y_le = _mm256_cmpgt_epi64(v_y_max, _mm256_sub_epi64(vy, _mm256_set1_epi64x(1)));
        __m256i y_valid = _mm256_and_si256(y_ge, y_le);

        __m256i z_ge = _mm256_cmpgt_epi64(vz, _mm256_sub_epi64(v_z_min, _mm256_set1_epi64x(1)));
        __m256i z_le = _mm256_cmpgt_epi64(v_z_max, _mm256_sub_epi64(vz, _mm256_set1_epi64x(1)));
        __m256i z_valid = _mm256_and_si256(z_ge, z_le);

        __m256i valid = _mm256_and_si256(x_valid, _mm256_and_si256(y_valid, z_valid));
        int mask = _mm256_movemask_pd(_mm256_castsi256_pd(valid));

        if (mask & 1) result_out[hit_count++] = keys[i + 0];
        if (mask & 2) result_out[hit_count++] = keys[i + 1];
        if (mask & 4) result_out[hit_count++] = keys[i + 2];
        if (mask & 8) result_out[hit_count++] = keys[i + 3];
    }

    if (i < count) {
        hit_count += search_sections_scalar(keys + i, count - i, x_min, x_max, y_min, y_max, z_min, z_max, result_out + hit_count);
    }
    return hit_count;
}