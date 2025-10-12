package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.apache.commons.collections4.IterableUtils;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanDeviceAndProperties;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public final class VulkanUtils {
   private static final Logger logger = LoggerFactory.getLogger(VulkanUtils.class);
   private VulkanUtils() {
   }

   public static void failIfNeeded(int resultCode, String errorMsg) {
      if (resultCode != VK14.VK_SUCCESS) {
         throw new RuntimeException(errorMsg + " result code: " + resultCode);
      }
   }

   public static List<VkPhysicalDevice> getPhysicalDevices(VkInstance instance, MemoryStack stack) {
      PointerBuffer devicesPointerBuffer;
      IntBuffer intBuffer = stack.mallocInt(1);
      failIfNeeded(VK14.vkEnumeratePhysicalDevices(instance, intBuffer, null), "Failed to get number of physical devices");
      int numDevices = intBuffer.get(0);
      logger.debug("Number of Physical Devices: {}", numDevices);

      devicesPointerBuffer = stack.mallocPointer(numDevices);
      failIfNeeded(VK14.vkEnumeratePhysicalDevices(instance, intBuffer, devicesPointerBuffer), "Failed to get physical devices");

      List<VkPhysicalDevice> physicalDevices = new ArrayList<>();
      for (int i = 0; i < numDevices; i++) {
         physicalDevices.add(new VkPhysicalDevice(devicesPointerBuffer.get(i), instance));
      }
      return physicalDevices;
   }

   public static VulkanDeviceAndProperties getPhysicalDeviceAndPropertiesByDeviceName(List<VkPhysicalDevice> devices, String deviceName) {
      VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc();
      VkPhysicalDevice matchingDevice = devices.stream().filter(d -> {
         VK14.vkGetPhysicalDeviceProperties(d, props);
         return (props.deviceNameString().equals(deviceName));
      }).findFirst().orElseThrow(() -> new RuntimeException("Unable to find physical device matching name: %s".formatted(deviceName)));
      return new VulkanDeviceAndProperties(matchingDevice, props);
   }

   public static int getGraphicsQueueFamilyIndex(VkQueueFamilyProperties.Buffer queueFamilyProperties) {
      return IterableUtils.indexOf(queueFamilyProperties, p -> (p.queueFlags() & VK14.VK_QUEUE_GRAPHICS_BIT) != 0);
   }

   public static int getPresentationQueueFamilyIndex(VkQueueFamilyProperties.Buffer queueFamilyProperties, VkPhysicalDevice physicalDevice, long surfaceId) {
      int matchingIndex = -1;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer buf = stack.mallocInt(1);
         for (int i = 0; i < queueFamilyProperties.capacity(); i++) {
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surfaceId, buf);
            if (buf.get(0) == VK14.VK_TRUE) {
               matchingIndex = i;
               break;
            }
         }
      }
      return matchingIndex;
   }

   public static void recordTransferCommand(VulkanCommandBuffer cmd, VulkanBuffer sourceBuffer, VulkanBuffer destinationBuffer) {
      try(MemoryStack stack = MemoryStack.stackPush()) {
         VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                 .srcOffset(0)
                 .dstOffset(0)
                 .size(sourceBuffer.getRequestedSize());
         VK14.vkCmdCopyBuffer(cmd.getBuffer(), sourceBuffer.getId(), destinationBuffer.getId(), copyRegion);
      }
   }

   public static VulkanBuffer createHostVisibleBuffer(LogicalDevice device, DescriptorAllocator allocator, long size, int usage, String id, DescriptorSetLayout layout) {
      VulkanBuffer buffer = new VulkanBuffer(device, size, usage, VK14.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK14.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      DescriptorSet set = allocator.addDescriptorSets(device, id, 1, layout).getFirst();
      set.setBuffer(device, buffer, buffer.getRequestedSize(), layout.getLayoutInfo().binding(), layout.getLayoutInfo().type());
      return buffer;
   }

   public static void copyMatrixToBuffer(VulkanBuffer buffer, Matrix4f matrix, int offset) {
      long mappedMem = buffer.map();
      ByteBuffer matrixBuffer = MemoryUtil.memByteBuffer(mappedMem, (int)buffer.getRequestedSize());
      matrix.get(offset, matrixBuffer);
      buffer.unmap();
   }
}

