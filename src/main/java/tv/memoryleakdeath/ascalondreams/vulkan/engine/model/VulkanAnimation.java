package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;

import java.util.List;

public record VulkanAnimation(String name, List<VulkanBuffer> frameBuffers) {
   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      frameBuffers.forEach(b -> b.cleanup(device, allocationUtil));
   }
}
