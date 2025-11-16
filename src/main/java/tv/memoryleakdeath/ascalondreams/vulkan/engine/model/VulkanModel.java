package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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

   public void addMesh(LogicalDevice device, String id, float[] verticies, int[] indicies) {
      TransferBuffer vertexBuffers = createVertexBuffers(device, verticies);
      TransferBuffer indexBuffers = createIndexBuffers(device, indicies);
      transferBuffers.add(vertexBuffers);
      transferBuffers.add(indexBuffers);
      meshList.add(new VulkanMesh(id, vertexBuffers.destinationBuffer(), indexBuffers.destinationBuffer(), indicies.length));
   }

   private TransferBuffer createVertexBuffers(LogicalDevice device, float[] verticies) {
      int numElements = verticies.length;
      int bufferSize = numElements * VulkanConstants.FLOAT_SIZE;

      var sourceBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      var destinationBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
      long mappedMemory = sourceBuffer.map(device);
      FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int)sourceBuffer.getRequestedSize());
      data.put(verticies);
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
}
