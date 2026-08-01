package dev.shiro8613.fixio.nativeapi.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class NativeUtils {

    private static boolean loaded = false;

    public static void ensureLoaded() {
        if (loaded) return;
        synchronized (NativeUtils.class) {
            if (!loaded) {
                loadLibrary("native_core");
                loaded = true;
            }
        }
    }

    private static synchronized void loadLibrary(String libName) {
        String os = getOsName();
        String arch = getArchName();
        String ext = getLibExtension();

        String fileName = System.mapLibraryName(libName);
        String resourcePath = "/natives/" + fileName;

        try (InputStream in = NativeUtils.class.getResourceAsStream(resourcePath)) {
            InputStream libStream = in;

            if (libStream == null) {
                String subPath = "/natives/" + os + "-" + arch + "/" + fileName;
                libStream = NativeUtils.class.getResourceAsStream(subPath);
            }

            if (libStream == null) {
                throw new FileNotFoundException("Native library not found in jar: " + resourcePath);
            }

            Path tempFile = Files.createTempFile(libName + "-", ext);
            tempFile.toFile().deleteOnExit();

            Files.copy(libStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.toAbsolutePath().toString());

        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library: " + libName, e);
        }
    }

    private static String getOsName() {
        String name = System.getProperty("os.name").toLowerCase();
        if (name.contains("win")) {
            return "windows";
        }
        if (name.contains("mac")) {
            return "macos";
        }
        if (name.contains("nux") || name.contains("nix")) {
            return "linux";
        }
        return "unknown";
    }

    private static String getArchName() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            return "x86_64";
        }
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        }
        return arch;
    }

    private static String getLibExtension() {
        String os = getOsName();
        if (os.equals("windows")) {
            return ".dll";
        }
        if (os.equals("macos")) {
            return ".dylib";
        }
        return ".so";
    }
}
