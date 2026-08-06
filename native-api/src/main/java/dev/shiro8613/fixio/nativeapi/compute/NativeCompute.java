package dev.shiro8613.fixio.nativeapi.compute;

import dev.shiro8613.fixio.nativeapi.utils.NativeUtils;

public class NativeCompute {

    static {
        NativeUtils.ensureLoaded();
    }

    public static int searchSections(
        long keysAddress, int inputCount,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        NativeReqCapArray resultBuffer
    ) {
        if (inputCount <= 0 || keysAddress == 0) return 0;

        long resultAddress = resultBuffer.ensureCapacity(inputCount);

        return searchSections(
            keysAddress, inputCount,
            minX, minY, minZ,
            maxX, maxY, maxZ,
            resultAddress
        );
    }

    private static native int searchSections(long keysAddress, int count, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, long resultAddress);
}
