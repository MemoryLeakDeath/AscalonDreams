package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class PipelineCache {
   private static final Logger logger = LoggerFactory.getLogger(PipelineCache.class);
   private LogicalDevice device;
   private long id;

   public PipelineCache(LogicalDevice device) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createPipelineCacheInfo(stack, device.getDevice());
         logger.debug("Created pipeline cache with id: {}", id);
      }
   }

   public void cleanup() {
      logger.debug("cleaning up pipeline cache: {}", id);
      VK14.vkDestroyPipelineCache(device.getDevice(), id, null);
   }

   public LogicalDevice getDevice() {
      return device;
   }

   public long getId() {
      return id;
   }
}
