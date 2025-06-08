package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class Pipeline {
   private static final int COLOR_MASK = VK14.VK_COLOR_COMPONENT_R_BIT | VK14.VK_COLOR_COMPONENT_G_BIT | VK14.VK_COLOR_COMPONENT_B_BIT | VK14.VK_COLOR_COMPONENT_A_BIT;
   private LogicalDevice device;
   private long id;
   private long layoutId;

   public Pipeline(PipelineCache cache, PipelineCreateInfo info) {
      this.device = cache.getDevice();
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkPipelineShaderStageCreateInfo.Buffer shaderStages = StructureUtils.createPipelineShaderInfo(stack, info.shaderProgram().getShaderModules(), "main");
         VkPipelineInputAssemblyStateCreateInfo assemblyStateCreateInfo = StructureUtils.createPipelineAssemblyStateInfo(stack, VK14.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
         VkPipelineViewportStateCreateInfo viewportStateCreateInfo = StructureUtils.createPipelineViewportStateInfo(stack, 1, 1);
         VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = StructureUtils.createRasterizationStateInfo(stack, VK14.VK_POLYGON_MODE_FILL,
                 VK14.VK_CULL_MODE_NONE, VK14.VK_FRONT_FACE_CLOCKWISE, 1.0f);
         VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo = StructureUtils.createMultisampleInfo(stack, VK14.VK_SAMPLE_COUNT_1_BIT);
         VkPipelineDepthStencilStateCreateInfo depthStencilStateCreateInfo = null;
         if (info.hasDepthAttachment()) {
            depthStencilStateCreateInfo = StructureUtils.createDepthStencilStateInfo(stack);
         }
         VkPipelineColorBlendStateCreateInfo colorBlendInfo = StructureUtils.createColorBlendStateInfo(stack, info.numColorAttachments(), COLOR_MASK);
         VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = StructureUtils.createDynamicStateInfo(stack, VK14.VK_DYNAMIC_STATE_VIEWPORT, VK14.VK_DYNAMIC_STATE_SCISSOR);
         VkPushConstantRange.Buffer constantRangeBuffer = null;
         if (info.pushConstantsSize() > 0) {
            constantRangeBuffer = StructureUtils.createConstantRangeBuffer(stack, info.pushConstantsSize());
         }
         this.layoutId = StructureUtils.createPipelineLayoutInfo(stack, device.getDevice(), constantRangeBuffer);
         this.id = StructureUtils.createPipelineInfo(stack, shaderStages,
                 info.vertexInputStateInfo(), assemblyStateCreateInfo, viewportStateCreateInfo,
                 rasterizationStateCreateInfo, multisampleStateCreateInfo, depthStencilStateCreateInfo, colorBlendInfo,
                 dynamicStateCreateInfo, layoutId, info.renderPass(), device.getDevice(), cache.getId());
      }
   }

   public void cleanup() {
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
