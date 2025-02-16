package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;

public class VulkanSwapChain {
   private final LogicalDevice device;

   public VulkanSwapChain(LogicalDevice device) {
      this.device = device;
   }
}
