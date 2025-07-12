package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkBufferCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.common.model.Mesh;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.SizeConstants;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

public class VulkanMesh {
   private static final Logger logger = LoggerFactory.getLogger(VulkanMesh.class);
   private Mesh mesh;
   private VulkanBuffer vertexBuffer;
   private VulkanBuffer indexBuffer;
   private List<TransferBuffers> transferBuffers;

   public VulkanMesh(Mesh mesh) {
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
      logger.debug("Created index buffer for mesh");

      return new TransferBuffers(sourceBuffer, destinationBuffer);
   }

   private TransferBuffers createVertexBuffer(LogicalDevice device) {
      float[] positions = mesh.getVertices();
      float[] textureCoords = mesh.getTexCoords().getFirst();
      if (textureCoords == null || textureCoords.length == 0) {
         textureCoords = new float[(positions.length / 3) * 2];
      }
      int numElements = textureCoords.length + positions.length;
      int bufferSize = numElements * SizeConstants.FLOAT_LENGTH;

      VulkanBuffer sourceBuffer = new VulkanBuffer(device, bufferSize,
              VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK14.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK14.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      VulkanBuffer destinationBuffer = new VulkanBuffer(device, bufferSize,
              VK14.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK14.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK14.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

      long mappedMemory = sourceBuffer.map();
      FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int) sourceBuffer.getRequestedSize());

      int rows = positions.length / 3;
      for (int row = 0; row < rows; row++) {
         int startPosition = row * 3;
         int startTexCoord = row * 2;
         data.put(positions[startPosition]);
         data.put(positions[startPosition + 1]);
         data.put(positions[startPosition + 2]);
         data.put(textureCoords[startTexCoord]);
         data.put(textureCoords[startTexCoord + 1]);
      }

      sourceBuffer.unmap();
      logger.debug("Created vertex buffer for mesh");

      return new TransferBuffers(sourceBuffer, destinationBuffer);
   }

   private void recordTransferCommands(VulkanCommandBuffer cmd, TransferBuffers vertexBuffer, TransferBuffers indexBuffer) {
      recordTransferCommand(cmd, vertexBuffer);
      recordTransferCommand(cmd, indexBuffer);
   }

   private void recordTransferCommand(VulkanCommandBuffer cmd, TransferBuffers buffers) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                 .srcOffset(0)
                 .dstOffset(0)
                 .size(buffers.source().getRequestedSize());
         VK14.vkCmdCopyBuffer(cmd.getBuffer(), buffers.source().getId(), buffers.destination().getId(), copyRegion);
      }
   }

   public void prepareMesh(LogicalDevice device, VulkanCommandBuffer commandBuffer) {
      TransferBuffers vertexBuffer = createVertexBuffer(device);
      TransferBuffers indexBuffer = createIndexBuffer(device);
      transferBuffers = List.of(vertexBuffer, indexBuffer);
      recordTransferCommands(commandBuffer, vertexBuffer, indexBuffer);
      this.vertexBuffer = vertexBuffer.destination();
      this.indexBuffer = indexBuffer.destination();
      logger.debug("Mesh prepared");
   }

   public void cleanup() {
      logger.debug("Mesh cleanup!");
      vertexBuffer.cleanup();
      indexBuffer.cleanup();
   }

   public void cleanSrcTransferBuffers() {
      transferBuffers.forEach(b -> b.source().cleanup());
   }

   public VulkanBuffer getVertexBuffer() {
      return vertexBuffer;
   }

   public VulkanBuffer getIndexBuffer() {
      return indexBuffer;
   }

   public Mesh getMesh() {
      return mesh;
   }

   private record TransferBuffers(VulkanBuffer source, VulkanBuffer destination) {
   }
}
