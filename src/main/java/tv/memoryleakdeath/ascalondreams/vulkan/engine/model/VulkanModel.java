package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class VulkanModel {
   private static final Logger logger = LoggerFactory.getLogger(VulkanModel.class);
   private String id;
   private List<VulkanMesh> meshList = new ArrayList<>();

   @JsonIgnore
   private List<TransferBuffer> transferBuffers = new ArrayList<>();

   public VulkanModel() {
   }

   public VulkanModel(String id) {
      this.id = id;
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      meshList.forEach(mesh -> mesh.cleanup(device, allocationUtil));
   }

   public String getId() {
      return id;
   }

   public List<VulkanMesh> getMeshList() {
      return meshList;
   }

   public void setId(String id) {
      this.id = id;
   }

   public void setMeshList(List<VulkanMesh> meshList) {
      this.meshList = meshList;
   }

   public List<TransferBuffer> getTransferBuffers() {
      return transferBuffers;
   }

   public void addMeshes(LogicalDevice device, MemoryAllocationUtil allocationUtil, List<VulkanMeshData> meshDataList) {
      for(VulkanMeshData data : meshDataList) {
         addMesh(device, allocationUtil, data.getId(), data);
      }
   }

   public void addMesh(LogicalDevice device, MemoryAllocationUtil allocationUtil, String id, VulkanMeshData meshData) {
      TransferBuffer vertexBuffers = createVertexBuffers(device, allocationUtil, meshData.getVerticies(), meshData.getTextureCoords());
      TransferBuffer indexBuffers = createIndexBuffers(device, allocationUtil, meshData.getIndicies());
      transferBuffers.add(vertexBuffers);
      transferBuffers.add(indexBuffers);
      meshList.add(new VulkanMesh(id, vertexBuffers.destinationBuffer(), indexBuffers.destinationBuffer(), meshData.getIndicies().length, meshData.getMaterialId()));
   }

   private TransferBuffer createVertexBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil, float[] verticies, float[] textureCoords) {
      if(textureCoords == null || textureCoords.length == 0) {
         textureCoords = new float[(verticies.length / 3) * 2];
      }
      int numElements = verticies.length + textureCoords.length;
      int bufferSize = numElements * VulkanConstants.FLOAT_SIZE;

      var sourceBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_AUTO,
              Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      var destinationBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
              Vma.VMA_MEMORY_USAGE_AUTO, 0, 0);
      long mappedMemory = sourceBuffer.map(device, allocationUtil);
      FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int)sourceBuffer.getRequestedSize());
      int rows = verticies.length / 3;
      for(int row = 0; row < rows; row++) {
         int vertexIndex = row * 3;
         int textureIndex = row * 2;
         data.put(ArrayUtils.subarray(verticies, vertexIndex, vertexIndex + 3));  // end index is exclusive thus + 3 instead of + 2
         data.put(ArrayUtils.subarray(textureCoords, textureIndex, textureIndex + 2)); // ditto (+2 instead of +1)
      }
      sourceBuffer.unMap(device, allocationUtil);
      return new TransferBuffer(sourceBuffer, destinationBuffer);
   }

   private TransferBuffer createIndexBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil, int[] indicies) {
      int numIndicies = indicies.length;
      int bufferSize = numIndicies * VulkanConstants.INT_SIZE;

      var sourceBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_AUTO,
              Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      var destinationBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
              Vma.VMA_MEMORY_USAGE_AUTO, 0, 0);
      long mappedMemory = sourceBuffer.map(device, allocationUtil);
      IntBuffer data = MemoryUtil.memIntBuffer(mappedMemory, (int)sourceBuffer.getRequestedSize());
      data.put(indicies);
      sourceBuffer.unMap(device, allocationUtil);
      return new TransferBuffer(sourceBuffer, destinationBuffer);
   }

   public void bindMeshes(MemoryStack stack, VkCommandBuffer cmd, long piplineLayoutId, Matrix4f modelMatrix, boolean doTransparent) {
      LongBuffer offsets = stack.mallocLong(1).put(0, 0L);
      LongBuffer vertexBuffer = stack.mallocLong(1);
      MaterialCache materialCache = MaterialCache.getInstance();
      AtomicInteger numRendered = new AtomicInteger(0);
      meshList.forEach(mesh -> {
         String materialId = mesh.materialId();
         int materialIndex = materialCache.getPosition(materialId);
         VulkanMaterial material = materialCache.getMaterial(materialId);
         if(!MaterialCache.getInstance().materialExists(materialId)) {
            logger.warn("Mesh {} in model {} does not have a material!", mesh.id(), id);
         } else {
            if(material.isTransparent() == doTransparent) {
               logger.trace("Rendering material: {}", materialId);
               numRendered.incrementAndGet();
               setPushConstants(cmd, modelMatrix, piplineLayoutId, materialIndex);
               vertexBuffer.put(0, mesh.vertexBuffer().getBuffer());
               VK13.vkCmdBindVertexBuffers(cmd, 0, vertexBuffer, offsets);
               VK13.vkCmdBindIndexBuffer(cmd, mesh.indexBuffer().getBuffer(), 0, VK13.VK_INDEX_TYPE_UINT32);
               VK13.vkCmdDrawIndexed(cmd, mesh.numIndicies(), 1, 0, 0, 0);
            }
         }
      });
      logger.debug("Render pass, transparency: {} rendered {} materials", doTransparent, numRendered.get());
   }

   private void setPushConstants(VkCommandBuffer cmd, Matrix4f modelMatrix, long pipelineLayoutId, int materialIndex) {
      ByteBuffer pushConstantsBuffer = VulkanPushConstantsHandler.getInstance();
      modelMatrix.get(0, pushConstantsBuffer);
      pushConstantsBuffer.putInt(VulkanConstants.MAT4X4_SIZE, materialIndex);
      VK13.vkCmdPushConstants(cmd, pipelineLayoutId, VK13.VK_SHADER_STAGE_VERTEX_BIT, 0,
              pushConstantsBuffer.slice(0, VulkanConstants.MAT4X4_SIZE));
      VK13.vkCmdPushConstants(cmd, pipelineLayoutId, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, VulkanConstants.MAT4X4_SIZE,
              pushConstantsBuffer.slice(VulkanConstants.MAT4X4_SIZE, VulkanConstants.INT_SIZE));
   }

}
