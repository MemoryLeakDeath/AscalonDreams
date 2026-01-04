package tv.memoryleakdeath.ascalondreams.buffers;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDrawIndirectCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.animations.AnimationCache;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.cache.MaterialCache;
import tv.memoryleakdeath.ascalondreams.cache.ModelCache;
import tv.memoryleakdeath.ascalondreams.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.scene.Entity;
import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GlobalBuffers {
   private static final Logger logger = LoggerFactory.getLogger(GlobalBuffers.class);
   public static final int IND_COMMAND_STRIDE = VkDrawIndirectCommand.SIZEOF;
   private static final int INSTANCE_DATA_SIZE = VulkanConstants.INT_SIZE * 2 + VulkanConstants.PTR_SIZE * 2;

   private final VulkanBuffer[] indirectDrawCommandsBuffer = new VulkanBuffer[VulkanConstants.MAX_IN_FLIGHT];
   private final VulkanBuffer[] instanceDataBuffer = new VulkanBuffer[VulkanConstants.MAX_IN_FLIGHT];
   private final VulkanBuffer[] modelMatricesBuffer = new VulkanBuffer[VulkanConstants.MAX_IN_FLIGHT];
   private final int[] drawCounts = new int[VulkanConstants.MAX_IN_FLIGHT];

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      Arrays.asList(indirectDrawCommandsBuffer).forEach(b -> b.cleanup(device, allocationUtil));
      Arrays.asList(modelMatricesBuffer).forEach(b -> b.cleanup(device, allocationUtil));
      Arrays.asList(instanceDataBuffer).forEach(b -> b.cleanup(device, allocationUtil));
   }

   private void createOrUpdateBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil,
                                      int frame, int numIndirectCommands, int numInstanceData, int numEntities) {
      boolean create = false;
      if(indirectDrawCommandsBuffer[frame] != null && drawCounts[frame] < numIndirectCommands) {
         indirectDrawCommandsBuffer[frame].cleanup(device, allocationUtil);
         create = true;
      }
      if(indirectDrawCommandsBuffer[frame] == null || create) {
         indirectDrawCommandsBuffer[frame] = new VulkanBuffer(device, allocationUtil, (long) IND_COMMAND_STRIDE * numIndirectCommands,
                 VK13.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT | VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                 Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT,
                 VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      }
      drawCounts[frame] = numIndirectCommands;

      create = false;
      int bufferSize = numInstanceData * INSTANCE_DATA_SIZE;
      if(instanceDataBuffer[frame] != null && instanceDataBuffer[frame].getRequestedSize() < bufferSize) {
         instanceDataBuffer[frame].cleanup(device, allocationUtil);
         create = true;
      }
      if(instanceDataBuffer[frame] == null || create) {
         instanceDataBuffer[frame] = new VulkanBuffer(device, allocationUtil, bufferSize,
                 VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE,
                 Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      }

      create = false;
      bufferSize = numEntities * VulkanConstants.MAT4X4_SIZE;
      if(modelMatricesBuffer[frame] != null && modelMatricesBuffer[frame].getRequestedSize() < bufferSize) {
         modelMatricesBuffer[frame].cleanup(device, allocationUtil);
         create = true;
      }
      if(modelMatricesBuffer[frame] == null || create) {
         modelMatricesBuffer[frame] = new VulkanBuffer(device, allocationUtil, bufferSize,
                 VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT, Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE,
                 Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      }
   }

   public long getInstanceDataAddress(int currentFrame) {
      return instanceDataBuffer[currentFrame].getAddress();
   }

   public long getModelMatricesAddress(int currentFrame) {
      return modelMatricesBuffer[currentFrame].getAddress();
   }

   public int getDrawCount(int currentFrame) {
      return drawCounts[currentFrame];
   }

   public VulkanBuffer getIndirectBuffer(int currentFrame) {
      return indirectDrawCommandsBuffer[currentFrame];
   }

   public void update(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanScene scene, int frame) {
      ModelCache modelCache = ModelCache.getInstance();
      try(var stack = MemoryStack.stackPush()) {
         Map<String, List<Entity>> entitiesMap = scene.getEntities();
         List<VkDrawIndirectCommand> commandList = new ArrayList<>();

         int baseEntityIndex = 0;
         int numInstanceData = 0;
         for(Map.Entry<String, List<Entity>> entry : entitiesMap.entrySet()) {
            int numEntities = entry.getValue().size();
            VulkanModel model = modelCache.getModel(entry.getKey());
            if(model.hasAnimations()) {
               for(var mesh : model.getMeshList()) {
                  for(int i = 0; i < numEntities; i++) {
                     var command = VkDrawIndirectCommand.calloc(stack)
                             .vertexCount(mesh.numIndicies())
                             .instanceCount(1)
                             .firstVertex(0)
                             .firstInstance(baseEntityIndex);
                     commandList.add(command);
                     baseEntityIndex++;
                     numInstanceData++;
                  }
               }
            } else {
               for(var mesh : model.getMeshList()) {
                  var command = VkDrawIndirectCommand.calloc(stack)
                          .vertexCount(mesh.numIndicies())
                          .instanceCount(numEntities)
                          .firstVertex(0)
                          .firstInstance(baseEntityIndex);
                  commandList.add(command);
                  baseEntityIndex += numEntities;
                  numInstanceData += numEntities;
               }
            }
         }
         int totalEntities = scene.getNumEntities();
         createOrUpdateBuffers(device, allocationUtil, frame, commandList.size(), numInstanceData, totalEntities);

         updateIndirectCommands(device, allocationUtil, commandList, frame);
         updateInstanceData(device, allocationUtil, entitiesMap, frame);
      }
   }

   private void updateIndirectCommands(LogicalDevice device, MemoryAllocationUtil allocationUtil,
                                       List<VkDrawIndirectCommand> commandList, int frame) {
      int numCommands = commandList.size();
      var buffer = indirectDrawCommandsBuffer[frame];
      ByteBuffer dataBuffer = MemoryUtil.memByteBuffer(buffer.map(device, allocationUtil), (int)buffer.getRequestedSize());
      VkDrawIndirectCommand.Buffer commandBuffer = new VkDrawIndirectCommand.Buffer(dataBuffer);
      for(int i = 0; i < numCommands; i++) {
         commandBuffer.put(i, commandList.get(i));
      }
      buffer.unMap(device, allocationUtil);
   }

   private void updateInstanceData(LogicalDevice device, MemoryAllocationUtil allocationUtil, Map<String, List<Entity>> entitiesMap,
                                   int frame) {
      ModelCache modelCache = ModelCache.getInstance();
      MaterialCache materialCache = MaterialCache.getInstance();
      AnimationCache animationCache = AnimationCache.getInstance();

      var instanceBuffer = instanceDataBuffer[frame];
      long mappedMemory = instanceBuffer.map(device, allocationUtil);
      ByteBuffer instanceData = MemoryUtil.memByteBuffer(mappedMemory, (int) instanceBuffer.getRequestedSize());

      var modelBuffer = modelMatricesBuffer[frame];
      mappedMemory = modelBuffer.map(device, allocationUtil);
      ByteBuffer matricesData = MemoryUtil.memByteBuffer(mappedMemory, (int) modelBuffer.getRequestedSize());

      int baseEntities = 0;
      int offset = 0;
      for(Map.Entry<String, List<Entity>> entry : entitiesMap.entrySet()) {
         List<Entity> entities = entry.getValue();
         String modelId = entry.getKey();
         int numEntities = entities.size();

         VulkanModel model = modelCache.getModel(modelId);
         for(var mesh : model.getMeshList()) {
            long vertexBufferAddress = mesh.vertexBuffer().getAddress();
            long indexBufferAddress = mesh.indexBuffer().getAddress();
            for(int i = 0; i < numEntities; i++) {
               Entity entity = entities.get(i);
               int entityIndex = baseEntities + i;
               entity.getModelMatrix().get(entityIndex * VulkanConstants.MAT4X4_SIZE, matricesData);

               instanceData.putInt(offset, entityIndex);
               offset += VulkanConstants.INT_SIZE;
               instanceData.putInt(offset, materialCache.getPosition(mesh.materialId()));
               offset += VulkanConstants.INT_SIZE;
               if(model.hasAnimations()) {
                  vertexBufferAddress = animationCache.getBuffer(entity.getId(), mesh.id()).getAddress();
               }
               instanceData.putLong(offset, vertexBufferAddress);
               offset += VulkanConstants.PTR_SIZE;
               instanceData.putLong(offset, indexBufferAddress);
               offset += VulkanConstants.PTR_SIZE;
            }
         }
         baseEntities += numEntities;
      }

      instanceBuffer.unMap(device, allocationUtil);
      modelBuffer.unMap(device, allocationUtil);
   }
}
