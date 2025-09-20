package tv.memoryleakdeath.ascalondreams.common.asset;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class BaseTexture {
    private static final Logger logger = LoggerFactory.getLogger(BaseTexture.class);
    protected String fileName;

    protected void loadTexture() {
        ByteBuffer buffer = null;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer width = memoryStack.mallocInt(1);
            IntBuffer height = memoryStack.mallocInt(1);
            IntBuffer channels = memoryStack.mallocInt(1);

            buffer = STBImage.stbi_load(fileName, width, height, channels, 4);
            if (buffer == null) {
                logger.error("Unable to load textures for file: {} Error Message: {}", fileName,
                        STBImage.stbi_failure_reason());
                throw new RuntimeException("Unable to load textures for file: %s, Error: %s".formatted(fileName,
                        STBImage.stbi_failure_reason()));
            }
            int mipLevels = 1;
            generateTexture(width.get(), height.get(), buffer, mipLevels);
        } finally {
            if(buffer != null) {
                STBImage.stbi_image_free(buffer);
            }
        }
    }

    protected abstract void generateTexture(int width, int height, ByteBuffer buffer, int mipLevels);

    public abstract void cleanup();

    public String getFileName() {
        return fileName;
    }
}
