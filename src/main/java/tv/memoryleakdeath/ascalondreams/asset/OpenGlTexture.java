package tv.memoryleakdeath.ascalondreams.asset;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.common.asset.BaseTexture;

public class OpenGlTexture extends BaseTexture {
    private static final Logger logger = LoggerFactory.getLogger(OpenGlTexture.class);

    private int id;
    private String path;

    public OpenGlTexture(String path) {
        this.path = path;
        loadTexture(path);
    }

    public int getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    protected void generateTexture(int width, int height, ByteBuffer buffer, int mipLevels) {
        this.id = GL46.glGenTextures();
        bind();
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR_MIPMAP_LINEAR);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);
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
