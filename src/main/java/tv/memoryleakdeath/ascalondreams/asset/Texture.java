package tv.memoryleakdeath.ascalondreams.asset;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Texture implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Texture.class);
    private static final long serialVersionUID = 1L;

    private int id;
    private String path;

    public Texture(String path) {
        this.path = path;
        loadTexture();
    }

    public int getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    private void loadTexture() {
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            IntBuffer width = memoryStack.mallocInt(1);
            IntBuffer height = memoryStack.mallocInt(1);
            IntBuffer channels = memoryStack.mallocInt(1);

            ByteBuffer buffer = STBImage.stbi_load(path, width, height, channels, 4);
            if (buffer == null) {
                logger.error("Unable to load textures for path: {} Error Message: {}", path,
                        STBImage.stbi_failure_reason());
                throw new RuntimeException("Unable to load textures for path: %s, Error: %s".formatted(path,
                        STBImage.stbi_failure_reason()));
            }
            generateTexture(width.get(), height.get(), buffer);
            STBImage.stbi_image_free(buffer);
        }
    }

    private void generateTexture(int width, int height, ByteBuffer buffer) {
        this.id = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, id);
        GL46.glPixelStorei(GL46.GL_UNPACK_ALIGNMENT, 1);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGBA, width, height, 0, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE,
                buffer);
        GL46.glGenerateMipmap(GL46.GL_TEXTURE_2D);
    }

    public void bind() {
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, id);
    }

    public void cleanup() {
        GL46.glDeleteTextures(id);
    }

}
