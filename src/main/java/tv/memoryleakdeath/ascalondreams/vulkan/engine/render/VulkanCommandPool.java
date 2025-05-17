package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class VulkanCommandPool {
   private final LogicalDevice device;
   private final long id;

   public VulkanCommandPool(LogicalDevice device, int queueFamilyIndex) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createPoolInfo(stack, device.getDevice(), queueFamilyIndex);
      }
   }

   public void cleanup() {
      VK14.vkDestroyCommandPool(device.getDevice(), id, null);
   }
   
   public LogicalDevice getDevice() {
      return device;
   }

   public long getId() {
      return id;
   }
}
