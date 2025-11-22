package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class Pipeline {
   private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);
   private final long id;
   private final long layoutId;

   public Pipeline(LogicalDevice device, PipelineCache pipelineCache, PipelineBuildInfo info) {
      logger.debug("Creating pipeline....");
      try (var stack = MemoryStack.stackPush()) {
         LongBuffer buf = stack.mallocLong(1);
         var shaderStages = info.createShaderStages(stack);
         var pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                 .sType$Default();
         VulkanUtils.failIfNeeded(VK13.vkCreatePipelineLayout(device.getDevice(), pipelineLayoutInfo, null, buf), "Failed to create pipeline layout");
         this.layoutId = buf.get(0);
         this.id = StructureUtils.createGraphicsPipelineInfo(stack, device, info.colorFormat(), shaderStages, info.info(), layoutId, pipelineCache);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up pipeline");
      VK13.vkDestroyPipelineLayout(device.getDevice(), layoutId, null);
      VK13.vkDestroyPipeline(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }

   public long getLayoutId() {
      return layoutId;
   }
}
