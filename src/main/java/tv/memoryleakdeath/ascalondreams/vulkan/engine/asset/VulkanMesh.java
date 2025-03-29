package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkBufferCopy;
import tv.memoryleakdeath.ascalondreams.common.model.Mesh;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.SizeConstants;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class VulkanMesh {
   private String id;
   private Mesh mesh;
   private VulkanBuffer vertexBuffer;
   private VulkanBuffer indexBuffer;

   public VulkanMesh(String id, Mesh mesh) {
      this.id = id;
      this.mesh = mesh;
   }

   private TransferBuffers createIndexBuffer(LogicalDevice device) {
      int bufferSize = mesh.getIndexes().length * SizeConstants.INT_LENGTH;
      VulkanBuffer sourceBuffer = new VulkanBuffer(device, bufferSize,
              VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK14.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK14.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      VulkanBuffer destinationBuffer = new VulkanBuffer(device, bufferSize,
              VK14.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK14.VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK14.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
      long mappedMemory = sourceBuffer.map();
      IntBuffer data = MemoryUtil.memIntBuffer(mappedMemory, (int) sourceBuffer.getRequestedSize());
      data.put(mesh.getIndexes());
      sourceBuffer.unmap();

      return new TransferBuffers(sourceBuffer, destinationBuffer);
   }

   private TransferBuffers createVertexBuffer(LogicalDevice device) {
      int bufferSize = mesh.getVertices().length * SizeConstants.FLOAT_LENGTH;
      VulkanBuffer sourceBuffer = new VulkanBuffer(device, bufferSize,
              VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK14.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK14.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      VulkanBuffer destinationBuffer = new VulkanBuffer(device, bufferSize,
              VK14.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK14.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK14.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
      long mappedMemory = sourceBuffer.map();
      FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int) sourceBuffer.getRequestedSize());
      data.put(mesh.getVertices());
      sourceBuffer.unmap();

      return new TransferBuffers(sourceBuffer, destinationBuffer);
   }

   private void recordTransferCommand(VulkanCommandBuffer cmd, TransferBuffers buffers) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1)
                 .srcOffset(0)
                 .dstOffset(0)
                 .size(buffers.source().getRequestedSize());
         VK14.vkCmdCopyBuffer(cmd.getBuffer(), buffers.source().getId(), buffers.destination().getId(), copyRegion);
      }
   }

   public void transformModels() {
      // TODO: complete this
   }

   public String getId() {
      return id;
   }

   public void cleanup() {
      vertexBuffer.cleanup();
      indexBuffer.cleanup();
   }

   private record TransferBuffers(VulkanBuffer source, VulkanBuffer destination) {
   }
}
