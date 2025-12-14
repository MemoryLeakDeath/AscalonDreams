package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.system.MemoryUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.nio.ByteBuffer;

public class VulkanPushConstantsHandler {
   private static final int PUSH_CONSTANTS_SIZE = VulkanConstants.MAT4X4_SIZE + VulkanConstants.INT_SIZE;
   private static ByteBuffer pushConstantsBuffer;

   private VulkanPushConstantsHandler() {
   }

   public static ByteBuffer getInstance() {
      if(pushConstantsBuffer == null) {
         pushConstantsBuffer = MemoryUtil.memAlloc(PUSH_CONSTANTS_SIZE);
      }
      return pushConstantsBuffer;
   }

   public static void free() {
      MemoryUtil.memFree(pushConstantsBuffer);
      pushConstantsBuffer = null;
   }

}
