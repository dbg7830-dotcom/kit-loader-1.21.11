package com.kitmod.util;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
public class ImageUtil {
    public static final int MAX_BYTES = 192 * 1024;
    public static String encodeFile(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            if (bytes.length > MAX_BYTES) return null;
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) { return null; }
    }
    public static boolean hasAWT() {
        try { Class.forName("java.awt.FileDialog"); return true; }
        catch (ClassNotFoundException e) { return false; }
    }
}
