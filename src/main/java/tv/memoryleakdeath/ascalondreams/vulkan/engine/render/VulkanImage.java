package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

public class VulkanImage {
   private LogicalDevice device;
   private VulkanImageData imageData;
   private long imageId;
   private long memoryId;

   public VulkanImage(LogicalDevice device, VulkanImageData imageData) {
      this.device = device;
      this.imageData = imageData;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.imageId = StructureUtils.createImageInfo(stack, device.getDevice(), imageData);
         this.memoryId = StructureUtils.allocateImageMemory(stack, device, imageId);
         VulkanUtils.failIfNeeded(VK14.vkBindImageMemory(device.getDevice(), imageId, memoryId, 0), "Failed to bind image memory!");
      }
   }

   public void cleanup() {
      VK14.vkDestroyImage(device.getDevice(), imageId, null);
      VK14.vkFreeMemory(device.getDevice(), memoryId, null);
   }

   public VulkanImageData getImageData() {
      return imageData;
   }

   public long getImageId() {
      return imageId;
   }

   public long getMemoryId() {
      return memoryId;
   }
}
