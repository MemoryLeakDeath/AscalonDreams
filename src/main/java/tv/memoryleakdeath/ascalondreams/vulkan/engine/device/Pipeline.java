package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pipeline.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

public class Pipeline {
   private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);
   private static final int COLOR_MASK = VK14.VK_COLOR_COMPONENT_R_BIT | VK14.VK_COLOR_COMPONENT_G_BIT | VK14.VK_COLOR_COMPONENT_B_BIT | VK14.VK_COLOR_COMPONENT_A_BIT;
   private LogicalDevice device;
   private long id;
   private long layoutId;

   public Pipeline(PipelineCache cache, PipelineBuildInfo info) {
      this.device = cache.getDevice();
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkPipelineShaderStageCreateInfo.Buffer shaderStages = StructureUtils.createPipelineShaderInfo(stack, info.getShaderModules(), "main");
         VkPipelineInputAssemblyStateCreateInfo assemblyStateCreateInfo = StructureUtils.createPipelineAssemblyStateInfo(stack, VK14.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
         VkPipelineViewportStateCreateInfo viewportStateCreateInfo = StructureUtils.createPipelineViewportStateInfo(stack, 1, 1);
         VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = StructureUtils.createRasterizationStateInfo(stack, VK14.VK_POLYGON_MODE_FILL,
                 VK14.VK_CULL_MODE_NONE, VK14.VK_FRONT_FACE_CLOCKWISE, 1.0f);
         VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo = StructureUtils.createMultisampleInfo(stack, VK14.VK_SAMPLE_COUNT_1_BIT);
         VkPipelineDepthStencilStateCreateInfo depthStencilStateCreateInfo = null;
         if (info.hasDepthFormat()) {
            depthStencilStateCreateInfo = StructureUtils.createDepthStencilStateInfo(stack);
         }
         VkPipelineColorBlendStateCreateInfo colorBlendInfo = StructureUtils.createColorBlendStateInfo(stack, 1, COLOR_MASK);
         VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = StructureUtils.createDynamicStateInfo(stack, VK14.VK_DYNAMIC_STATE_VIEWPORT, VK14.VK_DYNAMIC_STATE_SCISSOR);
         VkPushConstantRange.Buffer constantRangeBuffer = null;
         if (!info.getPushConstantRanges().isEmpty()) {
            constantRangeBuffer = StructureUtils.createConstantRangeBuffer(stack, info.getPushConstantRanges());
         }

         LongBuffer descriptorSetLayoutBuffer = null;
         if(!info.getDescriptorSetLayouts().isEmpty()) {
            descriptorSetLayoutBuffer = stack.mallocLong(info.getDescriptorSetLayouts().size());
            long[] ids = info.getDescriptorSetLayouts().stream().mapToLong(DescriptorSetLayout::getId).toArray();
            descriptorSetLayoutBuffer.put(ids);
         }

         VkPipelineRenderingCreateInfo pipelineRenderingCreateInfo = StructureUtils.createPipelineRenderingInfo(stack, info.getColorFormat());
         if(depthStencilStateCreateInfo != null) {
            pipelineRenderingCreateInfo.depthAttachmentFormat(info.getDepthFormat());
         }

         this.layoutId = StructureUtils.createPipelineLayoutInfo(stack, device.getDevice(), constantRangeBuffer);
         this.id = StructureUtils.createPipelineInfo(stack, shaderStages,
                 info.getVertexInputStateCreateInfo(), assemblyStateCreateInfo, viewportStateCreateInfo,
                 rasterizationStateCreateInfo, multisampleStateCreateInfo, depthStencilStateCreateInfo, colorBlendInfo,
                 dynamicStateCreateInfo, layoutId, VK14.VK_NULL_HANDLE, pipelineRenderingCreateInfo, device.getDevice(),
                 cache.getId());
         logger.debug("Created pipeline with id: {}", id);
      }
   }

   public void cleanup() {
      logger.debug("cleaning up pipeline: {}", id);
      VK14.vkDestroyPipelineLayout(device.getDevice(), layoutId, null);
      VK14.vkDestroyPipeline(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }

   public long getLayoutId() {
      return layoutId;
   }
}
