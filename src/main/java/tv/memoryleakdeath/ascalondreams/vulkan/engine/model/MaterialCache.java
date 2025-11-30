package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MaterialCache {
   private static final Logger logger = LoggerFactory.getLogger(MaterialCache.class);
   private static final int MATERIAL_SIZE = VulkanConstants.VEC4_SIZE * VulkanConstants.INT_SIZE * 4;

   private final Map<String, VulkanMaterial> materialMap = new LinkedHashMap<>();
   private VulkanBuffer materialsBuffer;

   public void cleanup(LogicalDevice device) {
      if(materialsBuffer != null) {
         materialsBuffer.cleanup(device);
      }
   }

   public VulkanMaterial getMaterial(String id) {
      return materialMap.get(id);
   }

   public VulkanBuffer getMaterialsBuffer() {
      return materialsBuffer;
   }

   public int getPosition(String id) {
      if(!materialMap.containsKey(id)) {
         logger.error("Could not find material with id: {}", id);
         return -1;
      }
      return List.copyOf(materialMap.keySet()).indexOf(id);
   }

   public void loadMaterials(LogicalDevice device, List<VulkanMaterial> materials, TextureCache textureCache, CommandPool pool, BaseDeviceQueue queue) {
      int bufferSize = MATERIAL_SIZE * materials.size();
      var sourceBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
              VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      materialsBuffer = new VulkanBuffer(device, bufferSize, VK13.VK_BUFFER_USAGE_TRANSFER_DST_BIT |VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
              VK13.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
      var transferBuffer = new TransferBuffer(sourceBuffer, materialsBuffer);
      ByteBuffer materialDataBuffer = MemoryUtil.memByteBuffer(sourceBuffer.map(device), (int)sourceBuffer.getRequestedSize());

      int offset = 0;
      for(VulkanMaterial material : materials) {
         material.load(materialDataBuffer, offset, textureCache);
         offset += MATERIAL_SIZE;
      }
      sourceBuffer.unMap(device);

      var cmd = new CommandBuffer(device, pool, true, true);
      cmd.beginRecording();
      transferBuffer.recordTransferCommand(cmd);
      cmd.endRecording();
      cmd.submitAndWait(device, queue);
      cmd.cleanup(device, pool);
      transferBuffer.sourceBuffer().cleanup(device);
   }
}
