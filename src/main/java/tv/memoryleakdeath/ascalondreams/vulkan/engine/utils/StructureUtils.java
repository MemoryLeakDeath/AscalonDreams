package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.apache.commons.exec.OS;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.common.CommonUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.TextureSampler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.TextureSamplerInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorTypeCount;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.LayoutInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pipeline.PushConstantRange;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.BaseVertexInputStateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImage;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.VulkanShaderProgram;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class StructureUtils {
   private static final Logger logger = LoggerFactory.getLogger(StructureUtils.class);
   private static final int VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR = 0x00000001;

   private StructureUtils() {
   }

   public static VkDevice createDevice(MemoryStack stack, PointerBuffer requiredExtensions, VkPhysicalDeviceFeatures features, VkDeviceQueueCreateInfo.Buffer queueCreationInfo, VkPhysicalDevice physicalDevice) {
      VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
              .ppEnabledExtensionNames(requiredExtensions)
              .pEnabledFeatures(features)
              .pQueueCreateInfos(queueCreationInfo);

      PointerBuffer logicalDevicePointer = stack.mallocPointer(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateDevice(physicalDevice, deviceCreateInfo, null, logicalDevicePointer), "Failed to create logical device!");
      return new VkDevice(logicalDevicePointer.get(0), physicalDevice, deviceCreateInfo);
   }

   public static VkDeviceQueueCreateInfo.Buffer initQueueFamilies(MemoryStack stack, VkQueueFamilyProperties.Buffer queueFamilyProperties) {
      int numQueueFamilies = queueFamilyProperties.capacity();
      VkDeviceQueueCreateInfo.Buffer queueCreationInfo = VkDeviceQueueCreateInfo.calloc(numQueueFamilies, stack);
      for (int i = 0; i < numQueueFamilies; i++) {
         FloatBuffer priorities = stack.callocFloat(queueFamilyProperties.get(i).queueCount());
         queueCreationInfo.get(i)
                 .sType(VK14.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                 .queueFamilyIndex(i)
                 .pQueuePriorities(priorities);
      }
      return queueCreationInfo;
   }

   private static Set<String> getDeviceExtensions(MemoryStack stack, VkPhysicalDevice physicalDevice) {
      IntBuffer numExtensionsBuf = stack.mallocInt(1);
      VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, numExtensionsBuf, null);
      int numExtensions = numExtensionsBuf.get(0);

      VkExtensionProperties.Buffer extensionProperties = VkExtensionProperties.calloc(numExtensions, stack);
      VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, numExtensionsBuf, extensionProperties);
      return extensionProperties.stream()
              .map(VkExtensionProperties::extensionNameString).collect(Collectors.toUnmodifiableSet());
   }

   public static PointerBuffer initRequiredExtensions(MemoryStack stack, VkPhysicalDevice physicalDevice) {
      Set<String> deviceExtensions = getDeviceExtensions(stack, physicalDevice);
      boolean usePortability = (deviceExtensions.contains(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME) && OS.isFamilyMac());
      int numExtensions = usePortability ? 2 : 1;
      PointerBuffer requiredExtensions = stack.mallocPointer(numExtensions);
      requiredExtensions.put(stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
      if (usePortability) {
         requiredExtensions.put(stack.ASCII(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME));
      }
      requiredExtensions.flip();
      return requiredExtensions;
   }

   public static VkExtensionProperties.Buffer getDeviceExtensionProperties(MemoryStack stack, VkPhysicalDevice physicalDevice) {
      IntBuffer intBuffer = stack.mallocInt(1);
      VulkanUtils.failIfNeeded(VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, intBuffer, null), "Failed to get number of device extension properties!");
      VkExtensionProperties.Buffer deviceExtensions = VkExtensionProperties.calloc(intBuffer.get(0));
      VulkanUtils.failIfNeeded(VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, intBuffer, deviceExtensions), "Failed to get device extension properties!");
      return deviceExtensions;
   }

   public static VkQueueFamilyProperties.Buffer getQueueFamilyProperties(MemoryStack stack, VkPhysicalDevice physicalDevice) {
      IntBuffer intBuffer = stack.mallocInt(1);
      VK14.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intBuffer, null);
      VkQueueFamilyProperties.Buffer queueFamilyProperties = VkQueueFamilyProperties.calloc(intBuffer.get(0));
      VK14.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intBuffer, queueFamilyProperties);
      return queueFamilyProperties;
   }

   public static VkPipelineShaderStageCreateInfo.Buffer createPipelineShaderInfo(MemoryStack stack, List<ShaderModule> shaderModules, String programName) {
      int numModules = shaderModules.size();
      ByteBuffer main = stack.UTF8(programName);
      VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(numModules, stack);
      for (int i = 0; i < numModules; i++) {
         shaderStages.get(i)
                 .sType$Default()
                 .stage(shaderModules.get(i).shaderStage())
                 .module(shaderModules.get(i).handle())
                 .pName(main);
      }
      return shaderStages;
   }

   public static VkPipelineInputAssemblyStateCreateInfo createPipelineAssemblyStateInfo(MemoryStack stack, int topology) {
      return VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
              .topology(topology);
   }

   public static VkPipelineViewportStateCreateInfo createPipelineViewportStateInfo(MemoryStack stack, int viewportCount, int scissorCount) {
      return VkPipelineViewportStateCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
              .viewportCount(viewportCount)
              .scissorCount(scissorCount);
   }

   public static VkPipelineRasterizationStateCreateInfo createRasterizationStateInfo(MemoryStack stack, int polygonMode, int cullMode, int frontFace, float lineWidth) {
      return VkPipelineRasterizationStateCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
              .polygonMode(polygonMode)
              .cullMode(cullMode)
              .frontFace(frontFace)
              .lineWidth(lineWidth);
   }

   public static VkPipelineMultisampleStateCreateInfo createMultisampleInfo(MemoryStack stack, int rasterizationSamples) {
      return VkPipelineMultisampleStateCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
              .rasterizationSamples(rasterizationSamples);
   }

   public static VkPipelineColorBlendStateCreateInfo createColorBlendStateInfo(MemoryStack stack, int numColorAttachments, final int colorWriteMask) {
      VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachBuffer = VkPipelineColorBlendAttachmentState.calloc(numColorAttachments, stack);
      colorBlendAttachBuffer.forEach(c -> c.colorWriteMask(colorWriteMask).blendEnable(false));

      return VkPipelineColorBlendStateCreateInfo.calloc(stack)
              .sType$Default()
              .pAttachments(colorBlendAttachBuffer);
   }

   public static VkPipelineDynamicStateCreateInfo createDynamicStateInfo(MemoryStack stack, int... dynamicStates) {
      return VkPipelineDynamicStateCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
              .pDynamicStates(stack.ints(dynamicStates));
   }

   public static long createPipelineLayoutInfo(MemoryStack stack, VkDevice device, VkPushConstantRange.Buffer constantRangeBuffer) {
      LongBuffer lb = stack.mallocLong(1);
      VkPipelineLayoutCreateInfo pipelineLayoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
              .pPushConstantRanges(constantRangeBuffer);
      VulkanUtils.failIfNeeded(VK14.vkCreatePipelineLayout(device, pipelineLayoutCreateInfo, null, lb), "Unable to create pipeline layout!");
      return lb.get(0);
   }

   public static long createPipelineInfo(MemoryStack stack,
                                         VkPipelineShaderStageCreateInfo.Buffer shaderStages,
                                         VkPipelineVertexInputStateCreateInfo vertexInputStateInfo,
                                         VkPipelineInputAssemblyStateCreateInfo assemblyStateCreateInfo,
                                         VkPipelineViewportStateCreateInfo viewportStateCreateInfo,
                                         VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo,
                                         VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo,
                                         VkPipelineDepthStencilStateCreateInfo depthStencilStateCreateInfo,
                                         VkPipelineColorBlendStateCreateInfo colorBlendInfo,
                                         VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo,
                                         long layoutId,
                                         long renderPass,
                                         VkPipelineRenderingCreateInfo renderingCreateInfo,
                                         VkDevice device,
                                         long cacheId) {
      LongBuffer lb = stack.mallocLong(1);
      VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
              .sType$Default()
              .pStages(shaderStages)
              .pVertexInputState(vertexInputStateInfo)
              .pInputAssemblyState(assemblyStateCreateInfo)
              .pViewportState(viewportStateCreateInfo)
              .pRasterizationState(rasterizationStateCreateInfo)
              .pMultisampleState(multisampleStateCreateInfo)
              .pColorBlendState(colorBlendInfo)
              .pDynamicState(dynamicStateCreateInfo)
              .layout(layoutId)
              .renderPass(renderPass)
              .pNext(renderingCreateInfo);
      if (depthStencilStateCreateInfo != null) {
         pipeline.pDepthStencilState(depthStencilStateCreateInfo);
      }
      VulkanUtils.failIfNeeded(VK14.vkCreateGraphicsPipelines(device, cacheId, pipeline, null, lb), "Failed to create graphics pipeline!");
      return lb.get(0);
   }

   public static long createPipelineCacheInfo(MemoryStack stack, VkDevice device) {
      VkPipelineCacheCreateInfo info = VkPipelineCacheCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);
      LongBuffer lb = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreatePipelineCache(device, info, null, lb), "Unable to create pipeline cache!");
      return lb.get(0);
   }

   public static long createSemaphoreInfo(MemoryStack stack, VkDevice device) {
      VkSemaphoreCreateInfo info = VkSemaphoreCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateSemaphore(device, info, null, buf), "Could not create semaphore!");
      return buf.get(0);
   }

   public static long createBufferInfo(MemoryStack stack, VkDevice device, long size, int usageType, int sharingMode) {
      VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
              .size(size)
              .usage(usageType)
              .sharingMode(sharingMode);
      LongBuffer lb = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateBuffer(device, bufferCreateInfo, null, lb), "Failed to create buffer structure!");
      return lb.get(0);
   }

   public static AllocateInfoResults createMemoryAllocateInfo(MemoryStack stack, VkDevice device, long allocationSize, int memoryTypeIndex) {
      LongBuffer lb = stack.mallocLong(1);
      VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
              .allocationSize(allocationSize)
              .memoryTypeIndex(memoryTypeIndex);
      VulkanUtils.failIfNeeded(VK14.vkAllocateMemory(device, allocateInfo, null, lb), "Failed to allocate memory for buffer!");
      return new AllocateInfoResults(allocateInfo.allocationSize(), lb.get(0));
   }

   public static PointerBuffer createCommandBufferAllocateInfo(MemoryStack stack, VkDevice device, long commandPoolId, boolean primary, int commandBufferCount) {
      VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
              .commandPool(commandPoolId)
              .level(primary ? VK14.VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK14.VK_COMMAND_BUFFER_LEVEL_SECONDARY)
              .commandBufferCount(commandBufferCount);
      PointerBuffer buf = stack.mallocPointer(1);
      VulkanUtils.failIfNeeded(VK14.vkAllocateCommandBuffers(device, allocateInfo, buf), "Cannot allocate command buffer!");
      return buf;
   }

   public static VkCommandBufferInheritanceInfo buildInheritanceInfo(MemoryStack stack, VulkanCommandBuffer.BufferInheritance inheritance) {
      if (inheritance == null) {
         throw new RuntimeException("Secondary Buffers must have inheritance information!");
      }
      VkCommandBufferInheritanceInfo info = VkCommandBufferInheritanceInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_INFO)
              .renderPass(inheritance.renderPassId())
              .subpass(inheritance.subPass())
              .framebuffer(inheritance.frameBufferId());
      return info;
   }

   public static long createPoolInfo(MemoryStack stack, VkDevice device, int queueFamilyIndex) {
      VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
              .flags(VK14.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
              .queueFamilyIndex(queueFamilyIndex);
      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateCommandPool(device, poolInfo, null, buf), "Cannot create command pool!");
      return buf.get(0);
   }

   public static long createFramebufferInfo(MemoryStack stack, VkDevice device, LongBuffer attachments, int width, int height, int numLayers, long renderPassId) {
      VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
              .pAttachments(attachments)
              .width(width)
              .height(height)
              .layers(numLayers)
              .renderPass(renderPassId);
      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateFramebuffer(device, framebufferCreateInfo, null, buf), "Cannot create framebuffer!");
      return buf.get(0);
   }

   public static long createImageViewInfo(MemoryStack stack, VkDevice device, long imagePointer, VulkanImageViewData data, int baseMipLevel) {
      LongBuffer imageViewPointer = stack.mallocLong(1);
      VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
              .image(imagePointer)
              .viewType(data.getViewType())
              .format(data.getFormat())
              .subresourceRange(it -> it
                      .aspectMask(data.getAspectMask())
                      .baseMipLevel(baseMipLevel)
                      .levelCount(data.getMipLevels())
                      .baseArrayLayer(data.getBaseArrayLayer())
                      .layerCount(data.getLayerCount()));
      VulkanUtils.failIfNeeded(VK14.vkCreateImageView(device, createInfo, null, imageViewPointer), "Unable to create image view!");
      return imageViewPointer.get(0);
   }

   public static VkApplicationInfo createApplicationInfo(MemoryStack stack, String appName, int appVersion, int engineVersion) {
      ByteBuffer appShortName = stack.UTF8(appName);
      VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_APPLICATION_INFO)
              .pApplicationName(appShortName)
              .applicationVersion(appVersion)
              .pEngineName(appShortName)
              .engineVersion(engineVersion)
              .apiVersion(VK14.VK_API_VERSION_1_4);
      return appInfo;
   }

   public static VkInstance createInstance(MemoryStack stack, long loggingExtension,
                                           VkApplicationInfo appInfo, PointerBuffer requiredLayers,
                                           PointerBuffer requiredExtensions, boolean usePortabilityExt) {
      VkInstanceCreateInfo instanceInfo = VkInstanceCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
              .pNext(loggingExtension)
              .pApplicationInfo(appInfo)
              .ppEnabledLayerNames(requiredLayers)
              .ppEnabledExtensionNames(requiredExtensions);
      if (usePortabilityExt) {
         instanceInfo.flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);
      }

      PointerBuffer instanceBuf = stack.mallocPointer(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateInstance(instanceInfo, null, instanceBuf), "Cannot create instance!");
      return new VkInstance(instanceBuf.get(0), instanceInfo);
   }

   public static long createSwapchainInfo(MemoryStack stack, long surfaceId, int minImageCount,
                                          int imageFormat, int colorSpace, VkExtent2D swapChainExtent,
                                          int imageArrayLayers, int preTransform, boolean clipped, boolean vsync,
                                          List<BaseDeviceQueue> concurrentQueues, VulkanPresentationQueue presentationQueue,
                                          VkDevice device) {
      VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
              .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
              .surface(surfaceId)
              .minImageCount(minImageCount)
              .imageFormat(imageFormat)
              .imageColorSpace(colorSpace)
              .imageExtent(swapChainExtent)
              .imageArrayLayers(imageArrayLayers)
              .imageUsage(VK14.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
              .preTransform(preTransform)
              .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
              .clipped(clipped);
      if (vsync) {
         swapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
      } else {
         swapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);
      }

      List<BaseDeviceQueue> queues = (concurrentQueues != null) ? concurrentQueues : Collections.emptyList();
      int presentationQueueFamilyIndex = presentationQueue.getQueueFamilyIndex();
      int[] queueIndexes = queues.stream()
              .filter(q -> q.getQueueFamilyIndex() != presentationQueueFamilyIndex)
              .mapToInt(BaseDeviceQueue::getQueueFamilyIndex)
              .toArray();
      if (queueIndexes.length > 0) {
         IntBuffer indexBuf = stack.mallocInt(queueIndexes.length + 1);
         indexBuf.put(queueIndexes);
         indexBuf.put(presentationQueueFamilyIndex).flip();
         swapchainCreateInfo.imageSharingMode(VK14.VK_SHARING_MODE_CONCURRENT)
                 .queueFamilyIndexCount(indexBuf.capacity())
                 .pQueueFamilyIndices(indexBuf);
      } else {
         swapchainCreateInfo.imageSharingMode(VK14.VK_SHARING_MODE_EXCLUSIVE);
      }
      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(KHRSwapchain.vkCreateSwapchainKHR(device, swapchainCreateInfo, null, buf), "Cannot create swapchain!");
      return buf.get(0);
   }

   public static VkPresentInfoKHR createPresentInfo(MemoryStack stack, int swapchainCount, long[] swapChainIds, int[] imageIndices, long[] semaphoreIds) {
      VkPresentInfoKHR presentInfoKHR = VkPresentInfoKHR.calloc(stack)
              .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
              .pWaitSemaphores(stack.longs(semaphoreIds))
              .swapchainCount(swapchainCount)
              .pSwapchains(stack.longs(swapChainIds))
              .pImageIndices(stack.ints(imageIndices));
      return presentInfoKHR;
   }

   public static long createRenderPass(MemoryStack stack, VkAttachmentDescription.Buffer attachments,
                                       VkSubpassDescription.Buffer subpassDescription, VkSubpassDependency.Buffer subpassDependencies,
                                       VkDevice device) {
      VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
              .pAttachments(attachments)
              .pSubpasses(subpassDescription)
              .pDependencies(subpassDependencies);

      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateRenderPass(device, renderPassInfo, null, buf), "Cannot create render pass!");
      return buf.get(0);
   }

   public static long createImageInfo(MemoryStack stack, VkDevice device, VulkanImageData imageData) {
      VkImageCreateInfo createInfo = VkImageCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
              .imageType(VK14.VK_IMAGE_TYPE_2D)
              .format(imageData.getFormat())
              .extent(it -> it.width(imageData.getWidth())
                      .height(imageData.getHeight())
                      .depth(1))
              .mipLevels(imageData.getMipLevels())
              .arrayLayers(imageData.getArrayLayers())
              .samples(imageData.getSampleCount())
              .initialLayout(VK14.VK_IMAGE_LAYOUT_UNDEFINED)
              .sharingMode(VK14.VK_SHARING_MODE_EXCLUSIVE)
              .tiling(VK14.VK_IMAGE_TILING_OPTIMAL)
              .usage(imageData.getUsage());
      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateImage(device, createInfo, null, buf), "Unable to create vulkan image!");
      return buf.get(0);
   }

   public static long allocateImageMemory(MemoryStack stack, LogicalDevice device, long imageId) {
      VkMemoryRequirements requirements = VkMemoryRequirements.calloc(stack);
      VK14.vkGetImageMemoryRequirements(device.getDevice(), imageId, requirements);

      VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
              .allocationSize(requirements.size())
              .memoryTypeIndex(device.getPhysicalDevice().getMemoryTypeFromProperties(requirements.memoryTypeBits(), 0));
      LongBuffer buf = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkAllocateMemory(device.getDevice(), allocateInfo, null, buf), "Unable to allocate image memory!");
      return buf.get(0);
   }

   public static VkPipelineDepthStencilStateCreateInfo createDepthStencilStateInfo(MemoryStack stack) {
      return VkPipelineDepthStencilStateCreateInfo.calloc(stack)
              .sType$Default()
              .depthTestEnable(true)
              .depthWriteEnable(true)
              .depthCompareOp(VK14.VK_COMPARE_OP_LESS_OR_EQUAL)
              .depthBoundsTestEnable(false)
              .stencilTestEnable(false);
   }

   public static VkPushConstantRange.Buffer createConstantRangeBuffer(MemoryStack stack, List<PushConstantRange> constantsSizes) {
      VkPushConstantRange.Buffer rangeBuffer = VkPushConstantRange.calloc(constantsSizes.size(), stack);
      for(PushConstantRange range : constantsSizes) {
         rangeBuffer.stageFlags(range.stage()).offset(range.offset()).size(range.size());
      }
      return rangeBuffer;
   }

   public static void recordCopyBuffer(MemoryStack stack, VulkanCommandBuffer cmd, VulkanBuffer data, VulkanImage image) {
      VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
              .bufferOffset(0)
              .bufferRowLength(0)
              .bufferImageHeight(0)
              .imageSubresource(ir -> ir.aspectMask(VK14.VK_IMAGE_ASPECT_COLOR_BIT)
                      .mipLevel(0).baseArrayLayer(0).layerCount(1))
              .imageOffset(off -> off.x(0).y(0).z(0))
              .imageExtent(ext -> ext.width(image.getImageData().getWidth())
                      .height(image.getImageData().getHeight()).depth(1));
      VK14.vkCmdCopyBufferToImage(cmd.getBuffer(), data.getId(), image.getImageId(), VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
   }

   public static void recordImageTransition(MemoryStack stack, VulkanCommandBuffer cmd, int oldLayout, int newLayout, VulkanImage image, int mipLevels) {
      VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
              .sType(VK14.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
              .oldLayout(oldLayout)
              .newLayout(newLayout)
              .srcQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
              .dstQueueFamilyIndex(VK14.VK_QUEUE_FAMILY_IGNORED)
              .image(image.getImageId())
              .subresourceRange(range -> range.aspectMask(VK14.VK_IMAGE_ASPECT_COLOR_BIT)
                      .baseMipLevel(0)
                      .levelCount(mipLevels)
                      .baseArrayLayer(0)
                      .layerCount(1));

      int srcStage;
      int srcAccessMask;
      int dstStage;
      int dstAccessMask;
      if(oldLayout == VK14.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
         srcStage = VK14.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
         srcAccessMask = 0;
         dstStage = VK14.VK_PIPELINE_STAGE_TRANSFER_BIT;
         dstAccessMask = VK14.VK_ACCESS_TRANSFER_WRITE_BIT;
      } else if(oldLayout == VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK14.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
         srcStage = VK14.VK_PIPELINE_STAGE_TRANSFER_BIT;
         srcAccessMask = VK14.VK_ACCESS_TRANSFER_WRITE_BIT;
         dstStage = VK14.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
         dstAccessMask = VK14.VK_ACCESS_SHADER_READ_BIT;
      } else {
         logger.error("Unknown layout transition");
         throw new RuntimeException("Unknown layout transition");
      }
      barrier.srcAccessMask(srcAccessMask);
      barrier.dstAccessMask(dstAccessMask);

      VK14.vkCmdPipelineBarrier(cmd.getBuffer(), srcStage, dstStage, 0,null,null,barrier);
   }

   public static long createDescriptorPoolInfo(MemoryStack stack, LogicalDevice device, int maxSets, List<DescriptorTypeCount> typeCounts) {
      VkDescriptorPoolSize.Buffer typeCountsBuffer = VkDescriptorPoolSize.calloc(typeCounts.size(), stack);
      for(DescriptorTypeCount count : typeCounts) {
         maxSets += count.count();
         typeCountsBuffer.get().type(count.type()).descriptorCount(count.count());
      }
      VkDescriptorPoolCreateInfo createInfo = VkDescriptorPoolCreateInfo.calloc(stack)
              .sType$Default()
              .flags(VK14.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
              .pPoolSizes(typeCountsBuffer)
              .maxSets(maxSets);
      LongBuffer memLoc = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateDescriptorPool(device.getDevice(), createInfo, null, memLoc), "Failed to create descriptor pool!");
      return memLoc.get(0);
   }

   public static long createDescriptorSetLayout(MemoryStack stack, LogicalDevice device, List<LayoutInfo> layoutInfoList) {
      VkDescriptorSetLayoutBinding.Buffer layoutBuffer = VkDescriptorSetLayoutBinding.calloc(layoutInfoList.size(), stack);
      for(LayoutInfo info : layoutInfoList) {
         layoutBuffer.get().binding(info.binding())
                 .descriptorType(info.type())
                 .descriptorCount(info.count())
                 .stageFlags(info.stage());
      }
      VkDescriptorSetLayoutCreateInfo createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
              .sType$Default()
              .pBindings(layoutBuffer);
      LongBuffer memLoc = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateDescriptorSetLayout(device.getDevice(), createInfo, null, memLoc), "Failed to create a descriptor set layout!");
      return memLoc.get(0);
   }

   public static long createTextureSamplerInfo(MemoryStack stack, LogicalDevice device, TextureSamplerInfo info, float maxAnsiotropy) {
      VkSamplerCreateInfo createInfo = VkSamplerCreateInfo.calloc(stack)
              .sType$Default()
              .magFilter(VK14.VK_FILTER_NEAREST)
              .minFilter(VK14.VK_FILTER_NEAREST)
              .addressModeU(info.addressMode())
              .addressModeV(info.addressMode())
              .addressModeW(info.addressMode())
              .borderColor(info.borderColor())
              .unnormalizedCoordinates(false)
              .compareEnable(false)
              .compareOp(VK14.VK_COMPARE_OP_NEVER)
              .mipmapMode(VK14.VK_SAMPLER_MIPMAP_MODE_NEAREST)
              .minLod(0f)
              .maxLod(info.mipLevels())
              .mipLodBias(0f);
      if(info.ansiotropy() && device.isSamplerAnsiotropy()) {
         createInfo.anisotropyEnable(true).maxAnisotropy(maxAnsiotropy);
      }

      LongBuffer memLoc = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkCreateSampler(device.getDevice(), createInfo, null, memLoc), "Failed to create texture sampler!");
      return memLoc.get(0);
   }

   public static long createDescriptorSet(MemoryStack stack, LogicalDevice device, long poolId, long layoutId) {
      LongBuffer layoutIdBuffer = stack.mallocLong(1);
      layoutIdBuffer.put(0, layoutId);
      VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.calloc(stack)
              .sType$Default()
              .descriptorPool(poolId)
              .pSetLayouts(layoutIdBuffer);
      LongBuffer memLoc = stack.mallocLong(1);
      VulkanUtils.failIfNeeded(VK14.vkAllocateDescriptorSets(device.getDevice(), allocateInfo, memLoc), "Failed to create a descriptor set!");
      return memLoc.get(0);
   }

   public static void updateDescriptorBuffer(MemoryStack stack, LogicalDevice device, VulkanBuffer buffer, long descriptorSetId, long range, int binding, int type) {
      VkDescriptorBufferInfo.Buffer descriptorBufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
              .buffer(buffer.getId())
              .offset(0)
              .range(range);
      VkWriteDescriptorSet.Buffer descriptorSetBuffer = VkWriteDescriptorSet.calloc(1, stack);
      descriptorSetBuffer.get(0)
              .sType$Default()
              .dstSet(descriptorSetId)
              .dstBinding(binding)
              .descriptorType(type)
              .descriptorCount(1)
              .pBufferInfo(descriptorBufferInfo);
      VK14.vkUpdateDescriptorSets(device.getDevice(), descriptorSetBuffer, null);
   }

   public static void updateDescriptorImageBuffer(MemoryStack stack, LogicalDevice device, long descriptorSetId, List<VulkanImageView> imageViews, long textureSamplerId, int baseBinding) {
      VkWriteDescriptorSet.Buffer descriptorSetBuffer = VkWriteDescriptorSet.calloc(imageViews.size(), stack);
      imageViews.forEach(CommonUtils.withIndex((index, imageView) -> {
         VkDescriptorImageInfo.Buffer imageInfoBuffer = VkDescriptorImageInfo.calloc(1, stack)
                  .imageView(imageView.getId())
                  .sampler(textureSamplerId);
         if(imageView.getData().isDepthImage()) {
            imageInfoBuffer.imageLayout(VK14.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
         } else {
            imageInfoBuffer.imageLayout(VK14.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
         }
         descriptorSetBuffer.get()
                 .sType$Default()
                 .dstSet(descriptorSetId)
                 .dstBinding(baseBinding + index)
                 .descriptorType(VK14.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                 .descriptorCount(1)
                 .pImageInfo(imageInfoBuffer);
      }));
      VK14.vkUpdateDescriptorSets(device.getDevice(), descriptorSetBuffer, null);
   }

   public static void updateDescriptorImageBufferArray(MemoryStack stack, LogicalDevice device, long descriptorSetId, List<VulkanImageView> imageViews, long textureSamplerId, int baseBinding) {
      VkDescriptorImageInfo.Buffer imageInfoBuffer = VkDescriptorImageInfo.calloc(imageViews.size(), stack);
      imageViews.forEach(CommonUtils.withIndex((index, imageView) -> {
         imageInfoBuffer.imageView(imageView.getId()).sampler(textureSamplerId);

         if(imageView.getData().isDepthImage()) {
            imageInfoBuffer.imageLayout(VK14.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
         } else {
            imageInfoBuffer.imageLayout(VK14.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
         }
      }));
      VkWriteDescriptorSet.Buffer descriptorSetBuffer = VkWriteDescriptorSet.calloc(1, stack);
      descriptorSetBuffer.get(0)
              .sType$Default()
              .dstSet(descriptorSetId)
              .dstBinding(baseBinding)
              .dstArrayElement(0)
              .descriptorType(VK14.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
              .descriptorCount(imageViews.size())
              .pImageInfo(imageInfoBuffer);
      VK14.vkUpdateDescriptorSets(device.getDevice(), descriptorSetBuffer, null);
   }

   public static VkPipelineRenderingCreateInfo createPipelineRenderingInfo(MemoryStack stack, int colorFormat) {
      IntBuffer colorFormatBuffer = stack.mallocInt(1);
      colorFormatBuffer.put(colorFormat);
      return VkPipelineRenderingCreateInfo.calloc(stack)
              .sType$Default()
              .colorAttachmentCount(1)
              .pColorAttachmentFormats(colorFormatBuffer);
   }
}
