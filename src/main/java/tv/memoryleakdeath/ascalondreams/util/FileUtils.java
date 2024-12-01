package tv.memoryleakdeath.ascalondreams.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
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

    public static final String readEntireFile(String file) {
        try {
            return Files.readString(Path.of(file));
        } catch (Exception e) {
            logger.error("Unable to open file: %s".formatted(file), e);
        }
        return null;
    }
}
