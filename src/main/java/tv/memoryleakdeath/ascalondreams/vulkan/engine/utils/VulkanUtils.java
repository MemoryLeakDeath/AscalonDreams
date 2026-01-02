package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkMemoryBarrier2;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanDeviceAndProperties;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public final class VulkanUtils {
   private static final Logger logger = LoggerFactory.getLogger(VulkanUtils.class);

   private VulkanUtils() {
   }

   public static void copyMatrixToBuffer(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanBuffer buffer, Matrix4f matrix, int offset) {
      long mappedMemory = buffer.map(device, allocationUtil);
      ByteBuffer matrixBuf = MemoryUtil.memByteBuffer(mappedMemory, (int)buffer.getRequestedSize());
      matrix.get(offset, matrixBuf);
      buffer.unMap(device, allocationUtil);
   }

   public static VulkanBuffer createHostVisibleBuffer(LogicalDevice device, MemoryAllocationUtil allocationUtil, DescriptorAllocator allocator, long size, int usage, String id, DescriptorSetLayout layout) {
      var buf = new VulkanBuffer(device, allocationUtil, size, usage,
              Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT,
              VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
      DescriptorSet set = allocator.addDescriptorSet(device, id, layout);
      set.setBuffer(device, buf, buf.getRequestedSize(), layout.getLayoutInfo().binding(), layout.getLayoutInfo().descriptorType());
      return buf;
   }

   public static List<VulkanBuffer> createHostVisibleBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil, DescriptorAllocator allocator, long size, int numBuffers, int usage, String id, DescriptorSetLayout layout) {
      List<VulkanBuffer> results = new ArrayList<>();
      allocator.addDescriptorSets(device, id, numBuffers, layout);
      var layoutInfo = layout.getLayoutInfo();
      for(int i = 0; i < numBuffers; i++) {
         var buf = new VulkanBuffer(device, allocationUtil, size, usage, Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE,
                 Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
         allocator.getDescriptorSet(id, i).setBuffer(device, buf, buf.getRequestedSize(), layoutInfo.binding(), layoutInfo.descriptorType());
         results.add(buf);
      }
      return results;
   }

   public static void failIfNeeded(int resultCode, String errorMsg) {
      if (resultCode != VK13.VK_SUCCESS) {
         throw new RuntimeException(errorMsg);
      }
   }

   public static List<VkPhysicalDevice> getPhysicalDevices(VkInstance instance, MemoryStack stack) {
      PointerBuffer devicesPointerBuffer;
      IntBuffer intBuffer = stack.mallocInt(1);
      failIfNeeded(VK13.vkEnumeratePhysicalDevices(instance, intBuffer, null), "Failed to get number of physical devices");
      int numDevices = intBuffer.get(0);
      logger.debug("Detected {} physical devices", numDevices);

      devicesPointerBuffer = stack.mallocPointer(numDevices);
      failIfNeeded(VK13.vkEnumeratePhysicalDevices(instance, intBuffer, devicesPointerBuffer), "Failed to get physical devices");

      List<VkPhysicalDevice> physicalDevices = new ArrayList<>();
      for (int i = 0; i < numDevices; i++) {
         physicalDevices.add(new VkPhysicalDevice(devicesPointerBuffer.get(i), instance));
      }
      return physicalDevices;
   }

   public static VulkanDeviceAndProperties getPhysicalDeviceAndPropertiesByDeviceName(List<VkPhysicalDevice> devices, String deviceName) {
      VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc();
      VkPhysicalDevice matchingDevice = devices.stream().filter(d -> {
         VK13.vkGetPhysicalDeviceProperties(d, props);
         return (props.deviceNameString().equals(deviceName));
      }).findFirst().orElseThrow(() -> new RuntimeException("Unable to find physical device matching name: %s".formatted(deviceName)));
      return new VulkanDeviceAndProperties(matchingDevice, props);
   }

   public static int getMemoryType(LogicalDevice device, int typeBits, int requirementsMask) {
      int result = -1;
      VkMemoryType.Buffer memoryTypes = device.getPhysicalDevice().getMemoryProperties().memoryTypes();
      for(int i = 0; i < VK13.VK_MAX_MEMORY_TYPES; i++) {
         if((typeBits & 1) == 1 && (memoryTypes.get(i).propertyFlags() & requirementsMask) == requirementsMask) {
            result = i;
            break;
         }
         typeBits >>= 1;
      }

      if(result < 0) {
         throw new RuntimeException("Memory type not found!");
      }
      return result;
   }

   public static void memoryBarrier(CommandBuffer commandBuffer, int sourceStageMask, int destinationStageMask,
                                    int sourceAccessMask, int destinationAccessMask, int dependencyFlags) {
      try(var stack = MemoryStack.stackPush()) {
         VkMemoryBarrier2.Buffer buffer = VkMemoryBarrier2.calloc(1, stack)
                 .sType$Default()
                 .srcStageMask(sourceStageMask)
                 .dstStageMask(destinationStageMask)
                 .srcAccessMask(sourceAccessMask)
                 .dstAccessMask(destinationAccessMask);
         var info = VkDependencyInfo.calloc(stack)
                 .sType$Default()
                 .pMemoryBarriers(buffer)
                 .dependencyFlags(dependencyFlags);

         VK13.vkCmdPipelineBarrier2(commandBuffer.getCommandBuffer(), info);
      }
   }
}

