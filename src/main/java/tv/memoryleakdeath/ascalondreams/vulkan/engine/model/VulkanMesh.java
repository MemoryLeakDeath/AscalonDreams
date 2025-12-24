package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;

public record VulkanMesh(String id, VulkanBuffer vertexBuffer, VulkanBuffer indexBuffer, int numIndicies, String materialId) {
   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      vertexBuffer.cleanup(device, allocationUtil);
      indexBuffer.cleanup(device, allocationUtil);
   }
}
