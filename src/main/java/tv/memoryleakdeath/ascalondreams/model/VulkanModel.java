package tv.memoryleakdeath.ascalondreams.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.model.conversion.AnimationMeshData;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanModel {
   private static final Logger logger = LoggerFactory.getLogger(VulkanModel.class);
   private String id;
   private List<VulkanMesh> meshList = new ArrayList<>();
   private List<VulkanAnimation> animationList = new ArrayList<>();

   @JsonIgnore
   private List<TransferBuffer> transferBuffers = new ArrayList<>();

   public VulkanModel() {
      // this is used for Json deserialization
   }

   public VulkanModel(String id) {
      this.id = id;
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      meshList.forEach(mesh -> mesh.cleanup(device, allocationUtil));
      animationList.forEach(a -> a.cleanup(device, allocationUtil));
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

   public List<VulkanAnimation> getAnimationList() {
      return animationList;
   }

   public boolean hasAnimations() {
      return !animationList.isEmpty();
   }

   public List<TransferBuffer> getTransferBuffers() {
      return transferBuffers;
   }

   public void addMeshes(LogicalDevice device, MemoryAllocationUtil allocationUtil, List<VulkanMeshData> meshDataList,
                         List<AnimationMeshData> animationMeshData) {
      int meshIndex = 0;
      boolean hasAnimationData = (animationMeshData != null && !animationMeshData.isEmpty());
      for(VulkanMeshData data : meshDataList) {
         addMesh(device, allocationUtil, data.getId(), data, hasAnimationData ? animationMeshData.get(meshIndex) : null);
         meshIndex++;
      }
   }

   public void addMesh(LogicalDevice device, MemoryAllocationUtil allocationUtil, String id, VulkanMeshData meshData,
                       AnimationMeshData animationMeshData) {
      TransferBuffer vertexBuffers = createVertexBuffers(device, allocationUtil, meshData.getVerticies(),
              meshData.getNormals(), meshData.getTangents(), meshData.getBiTangents(), meshData.getTextureCoords());
      TransferBuffer indexBuffers = createIndexBuffers(device, allocationUtil, meshData.getIndicies());
      TransferBuffer weightsBuffers = null;
      if(animationMeshData != null) {
         weightsBuffers = createWeightsBuffers(device, allocationUtil, animationMeshData);
      }
      logger.trace("Vertex buffers size: {} - Index buffers size: {}",
              vertexBuffers.sourceBuffer().getRequestedSize(),
              indexBuffers.sourceBuffer().getRequestedSize());
      if(weightsBuffers != null) {
         logger.trace("Weights buffers size: {}", weightsBuffers.sourceBuffer().getRequestedSize());
      }
      transferBuffers.add(vertexBuffers);
      transferBuffers.add(indexBuffers);
      if(weightsBuffers != null) {
         transferBuffers.add(weightsBuffers);
      }
      meshList.add(new VulkanMesh(id, vertexBuffers.destinationBuffer(), indexBuffers.destinationBuffer(),
              weightsBuffers != null ? weightsBuffers.destinationBuffer() : null,
              meshData.getIndicies().length, meshData.getMaterialId()));
   }

   public void addAnimations(LogicalDevice device, MemoryAllocationUtil allocationUtil, List<Animation> animations) {
      animations.forEach(animation -> {
         List<VulkanBuffer> frameBufferList = new ArrayList<>();
         VulkanAnimation vulkanAnimation = new VulkanAnimation(animation.name(), frameBufferList);
         animationList.add(vulkanAnimation);
         for(AnimatedFrame frame : animation.frames()) {
            TransferBuffer jointMatricesBuffers = createJointMatricesBuffers(device, allocationUtil, frame);
            transferBuffers.add(jointMatricesBuffers);
            frameBufferList.add(jointMatricesBuffers.destinationBuffer());
         }
      });
   }

   private TransferBuffer createVertexBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil,
                                              float[] verticies, float[] normals, float[] tangents,
                                              float[] bitangents, float[] textureCoords) {
      if(textureCoords == null || textureCoords.length == 0) {
         textureCoords = new float[(verticies.length / 3) * 2];
      }
      if(normals == null || normals.length == 0) {
         normals = new float[verticies.length];
      }
      if(tangents == null || tangents.length == 0) {
         tangents = new float[verticies.length];
      }
      if(bitangents == null || bitangents.length == 0) {
         bitangents = new float[verticies.length];
      }
      int numElements = verticies.length + normals.length + tangents.length + bitangents.length + textureCoords.length;
      int bufferSize = numElements * VulkanConstants.FLOAT_SIZE;

      var sourceBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, Vma.VMA_MEMORY_USAGE_AUTO,
              Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      var destinationBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
              | VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, Vma.VMA_MEMORY_USAGE_AUTO, 0, 0);
      long mappedMemory = sourceBuffer.map(device, allocationUtil);
      FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int)sourceBuffer.getRequestedSize());
      int rows = verticies.length / 3;
      for(int row = 0; row < rows; row++) {
         int vertexIndex = row * 3;
         int normalsIndex = row * 3;
         int tangentIndex = row * 3;
         int bitangetIndex = row * 3;
         int textureIndex = row * 2;
         data.put(ArrayUtils.subarray(verticies, vertexIndex, vertexIndex + 3));  // end index is exclusive thus + 3 instead of + 2
         data.put(ArrayUtils.subarray(normals, normalsIndex, normalsIndex + 3));  // end index is exclusive thus + 3 instead of + 2
         data.put(ArrayUtils.subarray(tangents, tangentIndex, tangentIndex + 3));  // end index is exclusive thus + 3 instead of + 2
         data.put(ArrayUtils.subarray(bitangents, bitangetIndex, bitangetIndex + 3));  // end index is exclusive thus + 3 instead of + 2
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
              VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT,
              Vma.VMA_MEMORY_USAGE_AUTO, Vma.VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT, 0);
      long mappedMemory = sourceBuffer.map(device, allocationUtil);
      IntBuffer data = MemoryUtil.memIntBuffer(mappedMemory, (int)sourceBuffer.getRequestedSize());
      data.put(indicies);
      sourceBuffer.unMap(device, allocationUtil);
      return new TransferBuffer(sourceBuffer, destinationBuffer);
   }

   private static TransferBuffer createJointMatricesBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil, AnimatedFrame frame) {
      List<Matrix4f> matrices = frame.jointMatrices();
      int numMatrices = matrices.size();
      int bufferSize = numMatrices * VulkanConstants.MAT4X4_SIZE;

      var sourceBuffer = new VulkanBuffer(device, allocationUtil, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
              Vma.VMA_MEMORY_USAGE_AUTO, Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT,
              VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      var destinationBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT,
              Vma.VMA_MEMORY_USAGE_AUTO, 0, 0);
      long mappedMemory = sourceBuffer.map(device, allocationUtil);
      ByteBuffer matrixBuffer = MemoryUtil.memByteBuffer(mappedMemory, (int) sourceBuffer.getRequestedSize());
      for(int i = 0; i < numMatrices; i++) {
         matrices.get(i).get(i * VulkanConstants.MAT4X4_SIZE, matrixBuffer);
      }
      sourceBuffer.unMap(device, allocationUtil);

      return new TransferBuffer(sourceBuffer, destinationBuffer);
   }

   private static TransferBuffer createWeightsBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil, AnimationMeshData animationMeshData) {
      List<Float> weights = animationMeshData.weights();
      List<Integer> boneIds = animationMeshData.boneIds();
      int bufferSize = weights.size() * VulkanConstants.FLOAT_SIZE + boneIds.size() * VulkanConstants.INT_SIZE;

      var sourceBuffer = new VulkanBuffer(device, allocationUtil, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
              Vma.VMA_MEMORY_USAGE_AUTO, Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT,
              VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      var destinationBuffer = new VulkanBuffer(device, allocationUtil, bufferSize,
              VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
              | VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, Vma.VMA_MEMORY_USAGE_AUTO, 0, 0);
      long mappedMemory = sourceBuffer.map(device, allocationUtil);
      FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int) sourceBuffer.getRequestedSize());

      int rows = weights.size() / 4;
      for(int row = 0; row < rows; row++) {
         int startPosition = row * 4;
         data.put(weights.get(startPosition));
         data.put(weights.get(startPosition + 1));
         data.put(weights.get(startPosition + 2));
         data.put(weights.get(startPosition + 3));
         data.put(boneIds.get(startPosition));
         data.put(boneIds.get(startPosition + 1));
         data.put(boneIds.get(startPosition + 2));
         data.put(boneIds.get(startPosition + 3));
      }
      sourceBuffer.unMap(device, allocationUtil);

      return new TransferBuffer(sourceBuffer, destinationBuffer);
   }

}
