package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanDeviceAndProperties;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public final class VulkanUtils {
   private static final Logger logger = LoggerFactory.getLogger(VulkanUtils.class);

   private VulkanUtils() {
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
}

