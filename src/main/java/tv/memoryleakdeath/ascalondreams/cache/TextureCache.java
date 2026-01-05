package tv.memoryleakdeath.ascalondreams.cache;

import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.render.gui.GuiTexture;
import tv.memoryleakdeath.ascalondreams.model.VulkanTexture;
import tv.memoryleakdeath.ascalondreams.pojo.ImageSource;
import tv.memoryleakdeath.ascalondreams.util.GraphicsUtils;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class TextureCache {
   private static final Logger logger = LoggerFactory.getLogger(TextureCache.class);
   public static final int MAX_TEXTURES = 100;
   public static final String PADDING_TEXTURE_PATH = "models/default/default.png";
   private final Map<String, VulkanTexture> textureMap = new LinkedHashMap<>();
   private static TextureCache textureCache;

   private TextureCache() {
   }

   public static TextureCache getInstance() {
      if(textureCache == null) {
         textureCache = new TextureCache();
      }
      return textureCache;
   }

   private VulkanTexture addTexture(LogicalDevice device, MemoryAllocationUtil allocationUtil, String id, ImageSource source, int format,
                                   GuiTexture guiTexture) {
      if(textureMap.size() > MAX_TEXTURES) {
         var e = new RuntimeException("Texture cache is full!");
         logger.error("Max texture limit reached! Limit: %d".formatted(MAX_TEXTURES), e);
         throw e;
      }
      if(textureMap.containsKey(id)) {
         return textureMap.get(id);
      }
      var texture = new VulkanTexture(device, allocationUtil, id, source, format, guiTexture);
      textureMap.put(id, texture);
      return texture;
   }

   public VulkanTexture addGuiTexture(GuiTexture guiTexture, int format) {
      ImageSource source = null;
      VulkanTexture texture = null;
      try {
         source = GraphicsUtils.loadImage(guiTexture.texturePath());
         texture = addTexture(DeviceManager.getDevice(), MemoryAllocationUtil.getInstance(), guiTexture.texturePath(), source, format, guiTexture);
      } catch (IOException e) {
         logger.error("Could not load GUI texture file: %s".formatted(guiTexture.texturePath()), e);
      } finally {
         if(source != null) {
            GraphicsUtils.cleanImageData(source);
         }
      }
      return texture;
   }

   public VulkanTexture addTexture(LogicalDevice device, MemoryAllocationUtil allocationUtil, String id, String texturePath, int format) {
      ImageSource source = null;
      VulkanTexture texture = null;
      try {
         source = GraphicsUtils.loadImage(texturePath);
         texture = addTexture(device, allocationUtil, id, source, format, null);
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

   public List<GuiTexture> getGuiTextures() {
      return textureMap.values().stream().map(VulkanTexture::getGuiTexture).filter(Objects::nonNull).toList();
   }

   public VulkanTexture getTexture(String texturePath) {
      return textureMap.get(texturePath.trim());
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
