package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayout;
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

   public static void copyMatrixToBuffer(LogicalDevice device, VulkanBuffer buffer, Matrix4f matrix, int offset) {
      long mappedMemory = buffer.map(device);
      ByteBuffer matrixBuf = MemoryUtil.memByteBuffer(mappedMemory, (int)buffer.getRequestedSize());
      matrix.get(offset, matrixBuf);
      buffer.unMap(device);
   }

   public static VulkanBuffer createHostVisibleBuffer(LogicalDevice device, DescriptorAllocator allocator, long size, int usage, String id, DescriptorSetLayout layout) {
      var buf = new VulkanBuffer(device, size, usage, VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      DescriptorSet set = allocator.addDescriptorSet(device, id, layout);
      set.setBuffer(device, buf, buf.getRequestedSize(), layout.getLayoutInfo().binding(), layout.getLayoutInfo().descriptorType());
      return buf;
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
}

