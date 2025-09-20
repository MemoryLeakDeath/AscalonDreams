package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class VulkanImageView {
   private final LogicalDevice device;
   private final VulkanImageViewData data;
   private final long id;

   public VulkanImageView(LogicalDevice device, long imagePointer, VulkanImageViewData data) {
      this.device = device;
      this.data = data;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createImageViewInfo(stack, device.getDevice(), imagePointer, data, 0);
      }
   }

   public void cleanup() {
      VK14.vkDestroyImageView(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }

   public VulkanImageViewData getData() {
      return data;
   }
}
