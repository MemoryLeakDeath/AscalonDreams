package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanImageView {
   private final LogicalDevice device;
   private final VulkanImageViewData data;
   private final long id;

   public VulkanImageView(LogicalDevice device, long imagePointer, VulkanImageViewData data) {
      this.device = device;
      this.data = data;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         LongBuffer imageViewPointer = stack.mallocLong(1);
         VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                 .image(imagePointer)
                 .viewType(data.getViewType())
                 .format(data.getFormat())
                 .subresourceRange(it -> it
                         .aspectMask(data.getAspectMask())
                         .baseMipLevel(0)
                         .levelCount(data.getMipLevels())
                         .baseArrayLayer(data.getBaseArrayLayer())
                         .layerCount(data.getLayerCount()));
         VulkanUtils.failIfNeeded(VK14.vkCreateImageView(device.getDevice(), createInfo, null, imageViewPointer), "Unable to create image view!");
         this.id = imageViewPointer.get(0);
      }
   }

   public void cleanup() {
      VK14.vkDestroyImageView(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
