package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class PipelineCache {
   private LogicalDevice device;
   private long id;

   public PipelineCache(LogicalDevice device) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createPipelineCacheInfo(stack, device.getDevice());
      }
   }

   public void cleanup() {
      VK14.vkDestroyPipelineCache(device.getDevice(), id, null);
   }

   public LogicalDevice getDevice() {
      return device;
   }

   public long getId() {
      return id;
   }
}
