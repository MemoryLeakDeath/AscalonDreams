package tv.memoryleakdeath.ascalondreams.model;

import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;

import java.util.List;

public record VulkanAnimation(String name, List<VulkanBuffer> frameBuffers) {
   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      frameBuffers.forEach(b -> b.cleanup(device, allocationUtil));
   }
}
