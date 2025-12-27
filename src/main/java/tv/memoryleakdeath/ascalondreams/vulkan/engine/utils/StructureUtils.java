package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo;
import org.lwjgl.vulkan.VkCommandBufferInheritanceRenderingInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan13Features;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.SecondaryCommandBufferInheritanceInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.PipelineCache;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public final class StructureUtils {
   private StructureUtils() {}

   public static Object[] createDeviceInfo(MemoryStack stack, PointerBuffer requiredExtensions, PhysicalDevice physicalDevice) {
      // enable all queue families
      var queuePropertiesBuffer = physicalDevice.getQueueFamilyProperties();
      int numFamilies = queuePropertiesBuffer.capacity();

      var queueCreationInfoBuf = VkDeviceQueueCreateInfo.calloc(numFamilies, stack);
      for(int i = 0; i < numFamilies; i++) {
         FloatBuffer priorities = stack.callocFloat(queuePropertiesBuffer.get(i).queueCount());
         queueCreationInfoBuf.get(i)
                 .sType$Default()
                 .queueFamilyIndex(i)
                 .pQueuePriorities(priorities);
      }

      var vulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack)
              .sType$Default()
              .dynamicRendering(true)
              .synchronization2(true);
      var vulkanFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack).sType$Default();

      var features = vulkanFeatures2.features();
      VkPhysicalDeviceFeatures supportedFeatures = physicalDevice.getDeviceFeatures();
      boolean samplerAnisotrophy = supportedFeatures.samplerAnisotropy();
      if(samplerAnisotrophy) {
         features.samplerAnisotropy(true);
      }

      vulkanFeatures2.pNext(vulkan13Features.address());

      VkDeviceCreateInfo info = VkDeviceCreateInfo.calloc(stack)
              .sType$Default()
              .pNext(vulkanFeatures2.address())
              .ppEnabledExtensionNames(requiredExtensions)
              .pQueueCreateInfos(queueCreationInfoBuf);
      return new Object[] {info, samplerAnisotrophy};
   }

   public static long createCommandBufferAllocateInfo(MemoryStack stack, VkDevice logicalDevice, long commandPoolId, boolean primary) {
      var allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
              .sType$Default()
              .commandPool(commandPoolId)
              .level(primary ? VK13.VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK13.VK_COMMAND_BUFFER_LEVEL_SECONDARY)
              .commandBufferCount(1);
      PointerBuffer pb = stack.mallocPointer(1);
      VulkanUtils.failIfNeeded(VK13.vkAllocateCommandBuffers(logicalDevice, allocateInfo, pb), "Failed to allocate render command buffer!");
      return pb.get(0);
   }

   public static VkCommandBufferInheritanceInfo createCommandBufferInheritanceInfo(MemoryStack stack, SecondaryCommandBufferInheritanceInfo info) {
      IntBuffer colorFormatsBuffer = stack.callocInt(info.colorFormats().size());
      info.colorFormats().forEach(colorFormatsBuffer::put);
      var renderingInfo = VkCommandBufferInheritanceRenderingInfo.calloc(stack)
              .sType$Default()
              .depthAttachmentFormat(info.depthFormat())
              .pColorAttachmentFormats(colorFormatsBuffer)
              .rasterizationSamples(info.rasterizationSamples());
      return VkCommandBufferInheritanceInfo.calloc(stack)
              .sType$Default()
              .pNext(renderingInfo);
   }

   public static long createCommandPool(MemoryStack stack, LogicalDevice device, int queueFamilyIndex, boolean supportReset) {
      var info = VkCommandPoolCreateInfo.calloc(stack)
              .sType$Default()
              .queueFamilyIndex(queueFamilyIndex);
      if(supportReset) {
         info.flags(VK13.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
      }

      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK13.vkCreateCommandPool(device.getDevice(), info, null, buf), "Failed to create command pool!");
      return buf.get(0);
   }

   public static void imageBarrier(MemoryStack stack, VkCommandBuffer commandHandle, long image, int oldLayout, int newLayout,
                                   long sourceStage, long destinationStage, long sourceAccess, long destinationAccess, int aspectMask) {
      var imageBarrier = VkImageMemoryBarrier2.calloc(1, stack)
              .sType$Default()
              .oldLayout(oldLayout)
              .newLayout(newLayout)
              .srcStageMask(sourceStage)
              .dstStageMask(destinationStage)
              .srcAccessMask(sourceAccess)
              .dstAccessMask(destinationAccess)
              .subresourceRange(r -> r.aspectMask(aspectMask)
                      .baseMipLevel(0)
                      .levelCount(VK13.VK_REMAINING_MIP_LEVELS)
                      .baseArrayLayer(0)
                      .layerCount(VK13.VK_REMAINING_ARRAY_LAYERS))
              .image(image);
      var dependencyInfo = VkDependencyInfo.calloc(stack)
              .sType$Default()
              .pImageMemoryBarriers(imageBarrier);
      VK13.vkCmdPipelineBarrier2(commandHandle, dependencyInfo);
   }

   public static void setupViewportAndScissor(MemoryStack stack, int width, int height, VkCommandBuffer cmd) {
      setupViewport(stack, width, height, cmd);
      var scissor = VkRect2D.calloc(1, stack)
              .extent(ex -> ex.width(width).height(height))
              .offset(off -> off.x(0).y(0));
      VK13.vkCmdSetScissor(cmd, 0, scissor);
   }

   public static void setupViewport(MemoryStack stack, int width, int height, VkCommandBuffer cmd) {
      var viewport = VkViewport.calloc(1, stack)
              .x(0)
              .y(height)
              .height(-height)
              .width(width)
              .minDepth(0f)
              .maxDepth(1f);
      VK13.vkCmdSetViewport(cmd, 0, viewport);
   }

   public static long createGraphicsPipelineInfo(MemoryStack stack, LogicalDevice device, int colorFormat, VkPipelineShaderStageCreateInfo.Buffer stageInfo, VkPipelineVertexInputStateCreateInfo vertexInfo, long pipelineLayoutId, PipelineCache pipelineCache, int depthFormat, boolean useBlend) {
      var assemblyStateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
              .sType$Default()
              .topology(VK13.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
      var viewportStateInfo = VkPipelineViewportStateCreateInfo.calloc(stack)
              .sType$Default()
              .viewportCount(1)
              .scissorCount(1);
      var rasterizationStateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack)
              .sType$Default()
              .polygonMode(VK13.VK_POLYGON_MODE_FILL)
              .cullMode(VK13.VK_CULL_MODE_NONE)
              .frontFace(VK13.VK_FRONT_FACE_CLOCKWISE)
              .lineWidth(1f);
      var mutisampleStateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack)
              .sType$Default()
              .rasterizationSamples(VK13.VK_SAMPLE_COUNT_1_BIT);
      VkPipelineDepthStencilStateCreateInfo depthStencilStateInfo = null;
      if(depthFormat != VK13.VK_FORMAT_UNDEFINED) {
         depthStencilStateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                 .sType$Default()
                 .depthTestEnable(true)
                 .depthWriteEnable(true)
                 .depthCompareOp(VK13.VK_COMPARE_OP_LESS_OR_EQUAL)
                 .depthBoundsTestEnable(false)
                 .stencilTestEnable(false);
      }
      var dynamicStateInfo = VkPipelineDynamicStateCreateInfo.calloc(stack)
              .sType$Default()
              .pDynamicStates(stack.ints(VK13.VK_DYNAMIC_STATE_VIEWPORT, VK13.VK_DYNAMIC_STATE_SCISSOR));
      var colorBlendStateInfo = createColorBlendStateInfo(stack, useBlend);

      IntBuffer colorFormats = stack.mallocInt(1);
      colorFormats.put(0, colorFormat);
      var renderingInfo = VkPipelineRenderingCreateInfo.calloc(stack)
              .sType$Default()
              .colorAttachmentCount(1)
              .pColorAttachmentFormats(colorFormats);
      if(depthStencilStateInfo != null) {
         renderingInfo.depthAttachmentFormat(depthFormat);
      }
      var info = VkGraphicsPipelineCreateInfo.calloc(1, stack)
              .sType$Default()
              .pStages(stageInfo)
              .pVertexInputState(vertexInfo)
              .pInputAssemblyState(assemblyStateInfo)
              .pViewportState(viewportStateInfo)
              .pRasterizationState(rasterizationStateInfo)
              .pColorBlendState(colorBlendStateInfo)
              .pMultisampleState(mutisampleStateInfo)
              .pDynamicState(dynamicStateInfo)
              .layout(pipelineLayoutId)
              .pNext(renderingInfo);
      if(depthStencilStateInfo != null) {
         info.pDepthStencilState(depthStencilStateInfo);
      }
      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK13.vkCreateGraphicsPipelines(device.getDevice(), pipelineCache.getId(), info, null, buf), "Failed to create graphics pipeline!");
      return buf.get(0);
   }

   public static VkPipelineColorBlendStateCreateInfo createColorBlendStateInfo(MemoryStack stack, boolean useBlend) {
      var blendAttachState = VkPipelineColorBlendAttachmentState.calloc(1, stack)
              .colorWriteMask(VK13.VK_COLOR_COMPONENT_R_BIT | VK13.VK_COLOR_COMPONENT_G_BIT | VK13.VK_COLOR_COMPONENT_B_BIT | VK13.VK_COLOR_COMPONENT_A_BIT)
              .blendEnable(useBlend);
      if(useBlend) {
         blendAttachState.get(0).colorBlendOp(VK13.VK_BLEND_OP_ADD)
                 .alphaBlendOp(VK13.VK_BLEND_OP_ADD)
                 .srcColorBlendFactor(VK13.VK_BLEND_FACTOR_SRC_ALPHA)
                 .dstColorBlendFactor(VK13.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                 .srcAlphaBlendFactor(VK13.VK_BLEND_FACTOR_SRC_ALPHA)
                 .dstAlphaBlendFactor(VK13.VK_BLEND_FACTOR_ZERO);
      }
      return VkPipelineColorBlendStateCreateInfo.calloc(stack)
              .sType$Default()
              .pAttachments(blendAttachState);
   }
}
