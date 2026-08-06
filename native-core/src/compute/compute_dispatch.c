#include "compute.h"

#if defined(_WIN32) || defined(_MSC_VER) || defined(__clang__)
  #include <intrin.h>
#endif

JNIEXPORT jint JNICALL Java_dev_shiro8613_fixio_nativeapi_compute_NativeCompute_searchSections(
    JNIEnv *env,
    jclass clazz,
    jlong keysAddr,
    jint count,
    jdouble minX, jdouble minY, jdouble minZ,
    jdouble maxX, jdouble maxY, jdouble maxZ,
    jlong resultAddr
) {
    if (count <= 0 || keysAddr == 0 || resultAddr == 0) {
        return 0;
    }

    const int64_t* keys = (const int64_t*)(uintptr_t)keysAddr;
    int64_t* result_out = (int64_t*)(uintptr_t)resultAddr;

    int x_min = pos_to_section_coord(minX - 2.0);
    int y_min = pos_to_section_coord(minY - 4.0);
    int z_min = pos_to_section_coord(minZ - 2.0);
    int x_max = pos_to_section_coord(maxX + 2.0);
    int y_max = pos_to_section_coord(maxY + 0.0);
    int z_max = pos_to_section_coord(maxZ + 2.0);

#if defined(__x86_64__) || defined(_M_X64)
  #if defined(_WIN32) || defined(_MSC_VER)
    int cpuInfo[4];
    __cpuidex(cpuInfo, 7, 0);
    int has_avx2     = (cpuInfo[1] & (1 << 5)) != 0;
    int has_avx512f  = (cpuInfo[1] & (1 << 16)) != 0;
    int has_avx512vl = (cpuInfo[1] & (1 << 31)) != 0;

    if (has_avx512f && has_avx512vl) {
        return search_sections_avx512(keys, count, x_min, x_max, y_min, y_max, z_min, z_max, result_out);
    }
    if (has_avx2) {
        return search_sections_avx2(keys, count, x_min, x_max, y_min, y_max, z_min, z_max, result_out);
    }
  #elif defined(__GNUC__)
    if (__builtin_cpu_supports("avx512f") && __builtin_cpu_supports("avx512vl")) {
        return search_sections_avx512(keys, count, x_min, x_max, y_min, y_max, z_min, z_max, result_out);
    }
    if (__builtin_cpu_supports("avx2")) {
        return search_sections_avx2(keys, count, x_min, x_max, y_min, y_max, z_min, z_max, result_out);
    }
  #endif
#endif

    return search_sections_scalar(keys, count, x_min, x_max, y_min, y_max, z_min, z_max, result_out);
}