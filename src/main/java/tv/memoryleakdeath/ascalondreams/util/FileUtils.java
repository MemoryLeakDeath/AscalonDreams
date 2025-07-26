package tv.memoryleakdeath.ascalondreams.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private FileUtils() {
    }

    public static boolean exists(String filename) {
        return Files.exists(Path.of(filename));
    }

    public static boolean allExists(List<String> filenames) {
        if(filenames.isEmpty()) {
            return false;
        }
        return filenames.stream().allMatch(f -> Files.exists(Path.of(f)));
    }

    public static boolean dirExists(String directory) {
        return Files.isDirectory(Path.of(directory));
    }

    public static String appendPathAndCheckExists(String path, String file) {
        String fullPath = path + File.pathSeparator + file;
        if(!Files.exists(Path.of(fullPath))) {
            logger.error("File does not exist: {}", fullPath);
            throw new RuntimeException("File not found: %s".formatted(fullPath));
        }
        return fullPath;
    }

    public static String getParentDirectory(String filename) {
        if (exists(filename)) {
            return new File(filename).getParent();
        }
        return null;
    }

    public static String readEntireFile(String file) {
        try {
            return Files.readString(Path.of(file));
        } catch (Exception e) {
            logger.error("Unable to open file: %s".formatted(file), e);
        }
        return null;
    }
}
