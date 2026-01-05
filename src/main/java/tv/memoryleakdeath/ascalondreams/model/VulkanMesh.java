package tv.memoryleakdeath.ascalondreams.model;

import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;

public record VulkanMesh(String id, VulkanBuffer vertexBuffer, VulkanBuffer indexBuffer, VulkanBuffer weightsBuffer,
                         int numIndicies, String materialId) {
   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      vertexBuffer.cleanup(device, allocationUtil);
      indexBuffer.cleanup(device, allocationUtil);
      if(weightsBuffer != null) {
         weightsBuffer.cleanup(device, allocationUtil);
      }
   }
}
