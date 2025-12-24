package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.ImageSource;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.GraphicsUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TextureCache {
   private static final Logger logger = LoggerFactory.getLogger(TextureCache.class);
   public static final int MAX_TEXTURES = 100;
   public static final String PADDING_TEXTURE_PATH = "models/default/default.png";
   private final Map<String, VulkanTexture> textureMap = new LinkedHashMap<>();

   public TextureCache() {

   }

   public VulkanTexture addTexture(LogicalDevice device, MemoryAllocationUtil allocationUtil, String id, ImageSource source, int format) {
      if(textureMap.size() > MAX_TEXTURES) {
         logger.error("Max texture limit reached! Limit: {}", MAX_TEXTURES);
         throw new RuntimeException("Texture cache is full");
      }
      if(textureMap.containsKey(id)) {
         return textureMap.get(id);
      }
      var texture = new VulkanTexture(device, allocationUtil, id, source, format);
      textureMap.put(id, texture);
      return texture;
   }

   public VulkanTexture addTexture(LogicalDevice device, MemoryAllocationUtil allocationUtil, String id, String texturePath, int format) {
      ImageSource source = null;
      VulkanTexture texture = null;
      try {
         source = GraphicsUtils.loadImage(texturePath);
         texture = addTexture(device, allocationUtil, id, source, format);
      } catch (IOException e) {
         logger.error("Could not load texture file: %s".formatted(texturePath), e);
      } finally {
         if(source != null) {
            GraphicsUtils.cleanImageData(source);
         }
      }
      return texture;
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      textureMap.forEach((k, v) -> v.cleanup(device, allocationUtil));
      textureMap.clear();
   }

   public List<VulkanTexture> getAsList() {
      return List.copyOf(textureMap.values());
   }

   public int getPosition(String id) {
      return List.copyOf(textureMap.keySet()).indexOf(id);
   }

   public void recordTextureTransitions(LogicalDevice device, MemoryAllocationUtil allocationUtil, CommandPool cmd, BaseDeviceQueue queue) {
      logger.debug("Recording texture transitions...");
      int numTextures = textureMap.size();
      if(numTextures < MAX_TEXTURES) {
         int paddingCount = MAX_TEXTURES - numTextures;
         for(int i = 0; i < paddingCount; i++) {
            addTexture(device, allocationUtil, UUID.randomUUID().toString(), PADDING_TEXTURE_PATH, VK13.VK_FORMAT_R8G8B8A8_SRGB);
         }
      }
      var commandBuffer = new CommandBuffer(device, cmd, true, true);
      commandBuffer.beginRecording();
      textureMap.forEach((k, v) -> v.recordTextureTransition(commandBuffer));
      commandBuffer.endRecording();
      commandBuffer.submitAndWait(device, queue);
      commandBuffer.cleanup(device, cmd);
      textureMap.forEach((k, v) -> v.cleanupStagingBuffer(device, allocationUtil));
      logger.debug("Recorded texture transitions!");
   }
}
