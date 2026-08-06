#ifndef COMPUTE_H
#define COMPUTE_H

#include <stdint.h>
#include <jni.h>

static inline int floor_double(double v) {
    int i = (int)v;
    return v < i ? i - 1 : i;
}

static inline int pos_to_section_coord(double pos) {
    return floor_double(pos) >> 4;
}

int search_sections_scalar(
    const int64_t* keys, int count,
    int x_min, int x_max, int y_min, int y_max, int z_min, int z_max,
    int64_t* result_out
);

int search_sections_avx2(
    const int64_t* keys, int count,
    int x_min, int x_max, int y_min, int y_max, int z_min, int z_max,
    int64_t* result_out
);

int search_sections_avx512(
    const int64_t* keys, int count,
    int x_min, int x_max, int y_min, int y_max, int z_min, int z_max,
    int64_t* result_out
);

#endif