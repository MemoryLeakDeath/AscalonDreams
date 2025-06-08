package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.apache.commons.exec.OS;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferInheritanceInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.BaseVertexInputStateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.VulkanShaderProgram;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class StructureUtils {
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

   public static VkPipelineShaderStageCreateInfo.Buffer createPipelineShaderInfo(MemoryStack stack, List<VulkanShaderProgram.ShaderModule> shaderModules, String programName) {
      int numModules = shaderModules.size();
      ByteBuffer main = stack.UTF8(programName);
      VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(numModules, stack);
      for (int i = 0; i < numModules; i++) {
         shaderStages.get(i)
                 .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
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
      colorBlendAttachBuffer.forEach(c -> c.colorWriteMask(colorWriteMask));

      return VkPipelineColorBlendStateCreateInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
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
                                         BaseVertexInputStateInfo vertexInputStateInfo,
                                         VkPipelineInputAssemblyStateCreateInfo assemblyStateCreateInfo,
                                         VkPipelineViewportStateCreateInfo viewportStateCreateInfo,
                                         VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo,
                                         VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo,
                                         VkPipelineDepthStencilStateCreateInfo depthStencilStateCreateInfo,
                                         VkPipelineColorBlendStateCreateInfo colorBlendInfo,
                                         VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo,
                                         long layoutId,
                                         long renderPass,
                                         VkDevice device,
                                         long cacheId) {
      LongBuffer lb = stack.mallocLong(1);
      VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
              .sType(VK14.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
              .pStages(shaderStages)
              .pVertexInputState(vertexInputStateInfo.getInfo())
              .pInputAssemblyState(assemblyStateCreateInfo)
              .pViewportState(viewportStateCreateInfo)
              .pRasterizationState(rasterizationStateCreateInfo)
              .pMultisampleState(multisampleStateCreateInfo)
              .pColorBlendState(colorBlendInfo)
              .pDynamicState(dynamicStateCreateInfo)
              .layout(layoutId)
              .renderPass(renderPass);
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
              .imageSharingMode(VK14.VK_SHARING_MODE_EXCLUSIVE)
              .preTransform(preTransform)
              .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
              .clipped(clipped);
      if (vsync) {
         swapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
      } else {
         swapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);
      }

      if (concurrentQueues != null) {
         int presentationQueueFamilyIndex = presentationQueue.getQueueFamilyIndex();
         int[] queueIndexes = concurrentQueues.stream()
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
              .sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
              .depthTestEnable(true)
              .depthWriteEnable(true)
              .depthCompareOp(VK14.VK_COMPARE_OP_LESS_OR_EQUAL)
              .depthBoundsTestEnable(false)
              .stencilTestEnable(false);
   }

   public static VkPushConstantRange.Buffer createConstantRangeBuffer(MemoryStack stack, int constantsSize) {
      return VkPushConstantRange.calloc(1, stack)
              .stageFlags(VK14.VK_SHADER_STAGE_VERTEX_BIT)
              .offset(0)
              .size(constantsSize);
   }
}
