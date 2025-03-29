package tv.memoryleakdeath.ascalondreams.common.model;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public abstract class BaseTexture {
   private static final Logger logger = LoggerFactory.getLogger(BaseTexture.class);
   private int id;
   private String path;

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

   protected abstract void generateTexture(int width, int height, ByteBuffer buffer);
}
