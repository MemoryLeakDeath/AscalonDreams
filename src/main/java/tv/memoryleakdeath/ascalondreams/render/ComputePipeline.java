package tv.memoryleakdeath.ascalondreams.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class ComputePipeline {
   private final Logger logger = LoggerFactory.getLogger(ComputePipeline.class);
   private final long id;
   private final long layoutId;

   public ComputePipeline(LogicalDevice device, PipelineCache pipelineCache, ShaderModule shaderModule, int pushConstantsSize,
                          List<DescriptorSetLayout> layouts) {
      logger.debug("Creating compute pipeline...");
      try(var stack = MemoryStack.stackPush()) {
         LongBuffer lp = stack.callocLong(1);
         ByteBuffer main = stack.UTF8("main");
         var shaderStage = VkPipelineShaderStageCreateInfo.calloc(stack)
                 .sType$Default()
                 .stage(shaderModule.getStage())
                 .module(shaderModule.getId())
                 .pName(main);
         if(shaderModule.getSpecializationInfo() != null) {
            shaderStage.pSpecializationInfo(shaderModule.getSpecializationInfo());
         }

         VkPushConstantRange.Buffer pushConstantBuffer = null;
         if(pushConstantsSize > 0) {
            pushConstantBuffer = VkPushConstantRange.calloc(1, stack)
                    .stageFlags(VK13.VK_SHADER_STAGE_COMPUTE_BIT)
                    .offset(0)
                    .size(pushConstantsSize);
         }

         int numLayouts = (layouts != null && !layouts.isEmpty()) ? layouts.size() : 0;
         LongBuffer layoutBuf = stack.mallocLong(numLayouts);
         for(int i = 0; i < numLayouts; i++) {
            layoutBuf.put(i, layouts.get(i).getId());
         }
         var pipelineInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                 .sType$Default()
                 .pSetLayouts(layoutBuf)
                 .pPushConstantRanges(pushConstantBuffer);
         VulkanUtils.failIfNeeded(VK13.vkCreatePipelineLayout(device.getDevice(), pipelineInfo, null, lp), "Failed to create compute pipeline layout");
         this.layoutId = lp.get(0);

         var computePipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack)
                 .sType$Default()
                 .stage(shaderStage)
                 .layout(layoutId);
         VulkanUtils.failIfNeeded(VK13.vkCreateComputePipelines(device.getDevice(), pipelineCache.getId(),
                 computePipelineInfo, null, lp), "Failed creating compute pipeline!");
         this.id = lp.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up compute pipeline");
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
