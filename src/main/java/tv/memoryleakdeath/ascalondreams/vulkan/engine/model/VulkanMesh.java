package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;

public record VulkanMesh(String id, VulkanBuffer vertexBuffer, VulkanBuffer indexBuffer, int numIndicies, String materialId) {
   public void cleanup(LogicalDevice device) {
      vertexBuffer.cleanup(device);
      indexBuffer.cleanup(device);
   }
}
