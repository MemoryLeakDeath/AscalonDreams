package tv.memoryleakdeath.ascalondreams.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    private FileUtils() {
    }

    public static final boolean exists(String filename) {
        return Files.exists(Path.of(filename));
    }

    public static final String getParentDirectory(String filename) {
        if (exists(filename)) {
            return new File(filename).getParent();
        }
        return null;
    }
}
