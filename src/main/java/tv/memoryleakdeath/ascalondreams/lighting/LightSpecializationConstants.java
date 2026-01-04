package tv.memoryleakdeath.ascalondreams.lighting;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkSpecializationInfo;
import org.lwjgl.vulkan.VkSpecializationMapEntry;
import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;

import java.nio.ByteBuffer;

public class LightSpecializationConstants {
   private final ByteBuffer data;
   private final VkSpecializationMapEntry.Buffer specializationEntryMap;
   private final VkSpecializationInfo specializationInfo;
   public static boolean SHADOW_DEBUG = false;

   public LightSpecializationConstants() {
      this.data = MemoryUtil.memAlloc(VulkanConstants.INT_SIZE * 2);
      data.putInt(VulkanScene.SHADOW_MAP_CASCADE_COUNT);
      data.putInt(SHADOW_DEBUG ? 1 : 0);
      data.flip();

      this.specializationEntryMap = VkSpecializationMapEntry.calloc(2);
      int offset = 0;
      specializationEntryMap.get(0)
              .constantID(0)
              .size(VulkanConstants.INT_SIZE)
              .offset(offset);
      offset += VulkanConstants.INT_SIZE;

      specializationEntryMap.get(1)
              .constantID(1)
              .size(VulkanConstants.INT_SIZE)
              .offset(offset);

      this.specializationInfo = VkSpecializationInfo.calloc()
              .pData(data)
              .pMapEntries(specializationEntryMap);
   }

   public void cleanup() {
      MemoryUtil.memFree(specializationEntryMap);
      specializationInfo.free();
      MemoryUtil.memFree(data);
   }

   public VkSpecializationInfo getSpecializationInfo() {
      return specializationInfo;
   }
}
