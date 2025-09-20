package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class TextureSampler {
   private static final int MAX_ANISOTROPY = 16;
   private final long id;

   public TextureSampler(LogicalDevice device, TextureSamplerInfo info) {
      try(MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createTextureSamplerInfo(stack, device, info, MAX_ANISOTROPY);
      }
   }

   public void cleanup(LogicalDevice device) {
      VK14.vkDestroySampler(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
