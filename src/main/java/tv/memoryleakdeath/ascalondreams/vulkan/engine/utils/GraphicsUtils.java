package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.ImageSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GraphicsUtils {
   private static final Logger logger = LoggerFactory.getLogger(GraphicsUtils.class);

   private GraphicsUtils() {
   }

   public static void cleanImageData(ImageSource sourceImage) {
      STBImage.stbi_image_free(sourceImage.data());
   }

   public static ImageSource loadImage(String filename) throws IOException {
      try (var stack = MemoryStack.stackPush()) {
         IntBuffer widthBuf = stack.mallocInt(1);
         IntBuffer heightBuf = stack.mallocInt(1);
         IntBuffer channelsBuf = stack.mallocInt(1);

         ByteBuffer buf = STBImage.stbi_load(filename, widthBuf, heightBuf, channelsBuf, 4);
         if(buf == null) {
            logger.error("Unable to load image file: {} cause: {}", filename, STBImage.stbi_failure_reason());
            throw new IOException("Image file: %s could not be loaded, reason: %s".formatted(filename, STBImage.stbi_failure_reason()));
         }
         return new ImageSource(buf, widthBuf.get(0), heightBuf.get(0), channelsBuf.get(0));
      }
   }
}
