package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanImageView {
   private final LogicalDevice device;
   private final VulkanImageViewData data;
   private final long imageId;
   private final long imageViewId;
   private final boolean depthImage;
   private final int layerCount;

   public VulkanImageView(LogicalDevice device, long imagePointer, VulkanImageViewData imageData, boolean depthImage) {
      this.device = device;
      this.data = imageData;
      this.imageId = imagePointer;
      this.depthImage = depthImage;
      this.layerCount = imageData.getLayerCount();

      try (MemoryStack stack = MemoryStack.stackPush()) {
         LongBuffer imageViewPointer = stack.mallocLong(1);
         VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
                 .sType$Default()
                 .image(imagePointer)
                 .viewType(data.getViewType())
                 .format(data.getFormat())
                 .subresourceRange(it -> it
                         .aspectMask(data.getAspectMask())
                         .baseMipLevel(0)
                         .levelCount(data.getMipLevels())
                         .baseArrayLayer(data.getBaseArrayLayer())
                         .layerCount(data.getLayerCount()));
         VulkanUtils.failIfNeeded(VK13.vkCreateImageView(device.getDevice(), createInfo, null, imageViewPointer), "Unable to create image view!");
         this.imageViewId = imageViewPointer.get(0);
      }
   }

   public void cleanup() {
      VK13.vkDestroyImageView(device.getDevice(), imageViewId, null);
   }

   public long getImageViewId() {
      return imageViewId;
   }

   public long getImageId() {
      return imageId;
   }

   public boolean isDepthImage() {
      return depthImage;
   }

   public int getLayerCount() {
      return layerCount;
   }
}
