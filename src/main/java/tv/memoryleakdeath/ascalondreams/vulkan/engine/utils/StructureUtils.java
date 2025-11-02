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
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceVulkan13Features;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.SecondaryCommandBufferInheritanceInfo;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public final class StructureUtils {
   private StructureUtils() {}

   public static VkDeviceCreateInfo createDeviceInfo(MemoryStack stack, PointerBuffer requiredExtensions, PhysicalDevice physicalDevice) {
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
      vulkanFeatures2.pNext(vulkan13Features);

      return VkDeviceCreateInfo.calloc(stack)
              .sType$Default()
              .pNext(vulkanFeatures2.address())
              .ppEnabledExtensionNames(requiredExtensions)
              .pQueueCreateInfos(queueCreationInfoBuf);
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
              .srcQueueFamilyIndex(VK13.VK_QUEUE_FAMILY_IGNORED)
              .dstQueueFamilyIndex(VK13.VK_QUEUE_FAMILY_IGNORED)
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
}
