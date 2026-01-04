package tv.memoryleakdeath.ascalondreams.postprocess;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkSpecializationInfo;
import org.lwjgl.vulkan.VkSpecializationMapEntry;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;

import java.nio.ByteBuffer;

public class SpecializationConstants {
   private static final boolean ENABLE_FXAA = true;
   private final ByteBuffer data;
   private final VkSpecializationMapEntry.Buffer specializationMapEntries;
   private final VkSpecializationInfo specializationInfo;

   public SpecializationConstants() {
      this.data = MemoryUtil.memAlloc(VulkanConstants.INT_SIZE);
      data.putInt(ENABLE_FXAA ? 1 : 0);
      data.flip();

      this.specializationMapEntries = VkSpecializationMapEntry.calloc(1);
      specializationMapEntries.get(0)
              .constantID(0)
              .size(VulkanConstants.INT_SIZE)
              .offset(0);

      this.specializationInfo = VkSpecializationInfo.calloc()
              .pData(data)
              .pMapEntries(specializationMapEntries);
   }

   public void cleanup() {
      MemoryUtil.memFree(specializationMapEntries);
      specializationInfo.free();
      MemoryUtil.memFree(data);
   }

   public VkSpecializationInfo getSpecializationInfo() {
      return specializationInfo;
   }
}
