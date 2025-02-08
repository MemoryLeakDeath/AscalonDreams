package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.vulkan.VK14;

public final class VulkanUtils {
   private VulkanUtils() {
   }

   public static void failIfNeeded(int resultCode, String errorMsg) {
      if (resultCode != VK14.VK_SUCCESS) {
         throw new RuntimeException(errorMsg);
      }
   }
}
