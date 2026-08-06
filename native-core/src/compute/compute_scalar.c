#include "compute.h"

int search_sections_scalar(
    const int64_t* keys, int count,
    int x_min, int x_max, int y_min, int y_max, int z_min, int z_max,
    int64_t* result_out
) {
    int hit_count = 0;
    for (int i = 0; i < count; i++) {
        int64_t node = keys[i];
        int x = (int)(node << 0 >> 42);
        int y = (int)(node << 44 >> 44);
        int z = (int)(node << 22 >> 42);

        if (x >= x_min && x <= x_max &&
            y >= y_min && y <= y_max &&
            z >= z_min && z <= z_max) {
            result_out[hit_count++] = node;
        }
    }
    return hit_count;
}