#include "compute.h"
#include <immintrin.h>

#if defined(_MSC_VER)
  #include <intrin.h>
#endif

int search_sections_avx512(
    const int64_t* keys, int count,
    int x_min, int x_max, int y_min, int y_max, int z_min, int z_max,
    int64_t* result_out
) {
    __m512i v_x_min = _mm512_set1_epi64(x_min);
    __m512i v_x_max = _mm512_set1_epi64(x_max);
    __m512i v_y_min = _mm512_set1_epi64(y_min);
    __m512i v_y_max = _mm512_set1_epi64(y_max);
    __m512i v_z_min = _mm512_set1_epi64(z_min);
    __m512i v_z_max = _mm512_set1_epi64(z_max);

    int hit_count = 0;
    int i = 0;

    for (; i <= count - 8; i += 8) {
        __m512i nodes = _mm512_loadu_si512((const __m512i*)(keys + i));

        __m512i vx = _mm512_srai_epi64(_mm512_slli_epi64(nodes, 0), 42);
        __m512i vy = _mm512_srai_epi64(_mm512_slli_epi64(nodes, 44), 44);
        __m512i vz = _mm512_srai_epi64(_mm512_slli_epi64(nodes, 22), 42);

        __mmask8 m_x = _mm512_cmpge_epi64_mask(vx, v_x_min) & _mm512_cmple_epi64_mask(vx, v_x_max);
        __mmask8 m_y = _mm512_cmpge_epi64_mask(vy, v_y_min) & _mm512_cmple_epi64_mask(vy, v_y_max);
        __mmask8 m_z = _mm512_cmpge_epi64_mask(vz, v_z_min) & _mm512_cmple_epi64_mask(vz, v_z_max);

        __mmask8 mask = m_x & m_y & m_z;

        _mm512_mask_compressstoreu_epi64(result_out + hit_count, mask, nodes);

#if defined(_MSC_VER)
        hit_count += (int)__popcnt((unsigned int)mask);
#else
        hit_count += __builtin_popcount((unsigned int)mask);
#endif
    }

    if (i < count) {
        hit_count += search_sections_scalar(keys + i, count - i, x_min, x_max, y_min, y_max, z_min, z_max, result_out + hit_count);
    }
    return hit_count;
}