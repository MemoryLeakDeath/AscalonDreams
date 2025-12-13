package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanModel {
   private String id;
   private List<VulkanMesh> meshList = new ArrayList<>();
   private List<TransferBuffer> transferBuffers = new ArrayList<>();

   public VulkanModel(String id) {
      this.id = id;
   }

   public void cleanup(LogicalDevice device) {
      meshList.forEach(mesh -> mesh.cleanup(device));
   }

   public String getId() {
      return id;
   }

   public List<VulkanMesh> getMeshList() {
      return meshList;
   }

   public List<TransferBuffer> getTransferBuffers() {
      return transferBuffers;
   }

   public void addMesh(LogicalDevice device, String id, VulkanMeshData meshData) {
      TransferBuffer vertexBuffers = createVertexBuffers(device, meshData.getVerticies(), meshData.getTextureCoords());
      TransferBuffer indexBuffers = createIndexBuffers(device, meshData.getIndicies());
      transferBuffers.add(vertexBuffers);
      transferBuffers.add(indexBuffers);
      meshList.add(new VulkanMesh(id, vertexBuffers.destinationBuffer(), indexBuffers.destinationBuffer(), meshData.getIndicies().length, meshData.getMaterialId()));
   }

   private TransferBuffer createVertexBuffers(LogicalDevice device, float[] verticies, float[] textureCoords) {
      if(textureCoords == null || textureCoords.length == 0) {
         textureCoords = new float[(verticies.length / 3) * 2];
      }
      int numElements = verticies.length + textureCoords.length;
      int bufferSize = numElements * VulkanConstants.FLOAT_SIZE;

      var sourceBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      var destinationBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
      long mappedMemory = sourceBuffer.map(device);
      FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int)sourceBuffer.getRequestedSize());
      int rows = verticies.length / 3;
      for(int row = 0; row < rows; row++) {
         int vertexIndex = row * 3;
         int textureIndex = row * 2;
         data.put(ArrayUtils.subarray(verticies, vertexIndex, vertexIndex + 3));  // end index is exclusive thus + 3 instead of + 2
         data.put(ArrayUtils.subarray(textureCoords, textureIndex, textureIndex + 2)); // ditto (+2 instead of +1)
      }
      sourceBuffer.unMap(device);
      return new TransferBuffer(sourceBuffer, destinationBuffer);
   }

   private TransferBuffer createIndexBuffers(LogicalDevice device, int[] indicies) {
      int numIndicies = indicies.length;
      int bufferSize = numIndicies * VulkanConstants.INT_SIZE;

      var sourceBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      var destinationBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
      long mappedMemory = sourceBuffer.map(device);
      IntBuffer data = MemoryUtil.memIntBuffer(mappedMemory, (int)sourceBuffer.getRequestedSize());
      data.put(indicies);
      sourceBuffer.unMap(device);
      return new TransferBuffer(sourceBuffer, destinationBuffer);
   }

   public void bindMeshes(MemoryStack stack, VkCommandBuffer cmd) {
      LongBuffer offsets = stack.mallocLong(1).put(0, 0L);
      LongBuffer vertexBuffer = stack.mallocLong(1);
      meshList.forEach(mesh -> {
         vertexBuffer.put(0, mesh.vertexBuffer().getBuffer());
         VK13.vkCmdBindVertexBuffers(cmd, 0, vertexBuffer, offsets);
         VK13.vkCmdBindIndexBuffer(cmd, mesh.indexBuffer().getBuffer(), 0, VK13.VK_INDEX_TYPE_UINT32);
         VK13.vkCmdDrawIndexed(cmd, mesh.numIndicies(), 1, 0, 0, 0);
      });
   }
}
