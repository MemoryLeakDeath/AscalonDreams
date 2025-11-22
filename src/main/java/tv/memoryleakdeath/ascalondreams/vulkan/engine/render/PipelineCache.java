package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class PipelineCache {
   private static final Logger logger = LoggerFactory.getLogger(PipelineCache.class);
   private final long id;

   public PipelineCache(LogicalDevice device) {
      logger.debug("Creating pipeline cache....");
      try(var stack = MemoryStack.stackPush()) {
         var info = VkPipelineCacheCreateInfo.calloc(stack).sType$Default();
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreatePipelineCache(device.getDevice(), info, null, buf), "Failed to create pipeline cache!");
         this.id = buf.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up pipeline cache");
      VK13.vkDestroyPipelineCache(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
