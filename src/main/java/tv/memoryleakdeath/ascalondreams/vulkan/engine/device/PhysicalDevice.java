package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.apache.commons.collections4.IterableUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkMemoryType;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSurface;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.IntBuffer;
import java.util.List;

public final class PhysicalDevice {
   private VkExtensionProperties.Buffer deviceExtensions;
   private VkPhysicalDeviceMemoryProperties memoryProperties;
   private final VkPhysicalDevice physicalDevice;
   private VkPhysicalDeviceFeatures deviceFeatures;
   private VkPhysicalDeviceProperties deviceProperties;
   private VkQueueFamilyProperties.Buffer queueFamilyProperties;

   private PhysicalDevice(VkPhysicalDevice physicalDevice) {
      this.physicalDevice = physicalDevice;
      init();
   }

   private PhysicalDevice(VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties properties) {
      this.physicalDevice = physicalDevice;
      this.deviceProperties = properties;
      init();
   }

   private void init() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer intBuffer = stack.mallocInt(1);
         if (deviceProperties == null) {
            deviceProperties = VkPhysicalDeviceProperties.calloc();
            VK14.vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);
         }

         VulkanUtils.failIfNeeded(VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, intBuffer, null), "Failed to get number of device extension properties!");
         deviceExtensions = VkExtensionProperties.calloc(intBuffer.get(0));
         VulkanUtils.failIfNeeded(VK14.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, intBuffer, deviceExtensions), "Failed to get device extension properties!");

         VK14.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intBuffer, null);
         queueFamilyProperties = VkQueueFamilyProperties.calloc(intBuffer.get(0));
         VK14.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intBuffer, queueFamilyProperties);

         deviceFeatures = VkPhysicalDeviceFeatures.calloc();
         VK14.vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);

         memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
         VK14.vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
      }
   }

   public static PhysicalDevice getInstanceForDeviceName(List<VkPhysicalDevice> devices, String deviceName) {
      VulkanDeviceAndProperties deviceAndProperties = VulkanUtils.getPhysicalDeviceAndPropertiesByDeviceName(devices, deviceName);
      return new PhysicalDevice(deviceAndProperties.physicalDevice(), deviceAndProperties.deviceProperties());
   }

   public static PhysicalDevice getInstance(VkInstance instance) {
      PhysicalDevice selectedDevice = null;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         List<VkPhysicalDevice> devices = VulkanUtils.getPhysicalDevices(instance, stack);
         if (devices.isEmpty()) {
            throw new RuntimeException("No physical devices found!");
         }
         for (VkPhysicalDevice device : devices) {
            selectedDevice = new PhysicalDevice(device);
            if (selectedDevice.isGameReady()) {
               break;
            }
            selectedDevice.cleanup();
            selectedDevice = null;
         }
      }
      if (selectedDevice == null) {
         throw new RuntimeException("No suitable physical devices were found!");
      }
      return selectedDevice;
   }

   public void cleanup() {
      memoryProperties.free();
      deviceFeatures.free();
      queueFamilyProperties.free();
      deviceExtensions.free();
      deviceProperties.free();
   }

   public String getDeviceName() {
      return deviceProperties.deviceNameString();
   }

   public boolean isGameReady() {
      return (hasGraphicsQueueFamily() && hasKHRSwapChainExtension());
   }

   private boolean hasGraphicsQueueFamily() {
      return queueFamilyProperties.stream()
              .anyMatch(p -> (p.queueFlags() & VK14.VK_QUEUE_GRAPHICS_BIT) != 0);
   }

   private boolean hasKHRSwapChainExtension() {
      return deviceExtensions.stream()
              .anyMatch(e -> KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(e.extensionNameString()));
   }

   public VkExtensionProperties.Buffer getDeviceExtensions() {
      return deviceExtensions;
   }

   public VkPhysicalDeviceMemoryProperties getMemoryProperties() {
      return memoryProperties;
   }

   public VkPhysicalDevice getPhysicalDevice() {
      return physicalDevice;
   }

   public VkPhysicalDeviceFeatures getDeviceFeatures() {
      return deviceFeatures;
   }

   public VkPhysicalDeviceProperties getDeviceProperties() {
      return deviceProperties;
   }

   public VkQueueFamilyProperties.Buffer getQueueFamilyProperties() {
      return queueFamilyProperties;
   }

   public int getGraphicsQueueIndex() {
      int index = IterableUtils.indexOf(queueFamilyProperties, p -> (p.queueFlags() & VK14.VK_QUEUE_GRAPHICS_BIT) != 0);
      if (index < 0) {
         throw new RuntimeException("No graphics queue family found!");
      }
      return index;
   }

   public int getPresentationQueueIndex(VulkanSurface surface) {
      int matchingIndex = -1;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer buf = stack.mallocInt(1);
         for (int i = 0; i < queueFamilyProperties.capacity(); i++) {
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface.getId(), buf);
            if (buf.get(0) == VK14.VK_TRUE) {
               matchingIndex = i;
               break;
            }
         }
      }
      if (matchingIndex < 0) {
         throw new RuntimeException("Unable to find any presentation queue family index!");
      }
      return matchingIndex;
   }

   public int getMemoryTypeFromProperties(int type, int mask) {
      VkMemoryType.Buffer memoryTypes = memoryProperties.memoryTypes();
      for (int i = 0; i < VK14.VK_MAX_MEMORY_TYPES; i++) {
         if ((((type >> i) & 1) == 1) && (memoryTypes.get(i).propertyFlags() & mask) == mask) {
            return i;
         }
      }
      throw new RuntimeException("Failed to find memory type!");
   }
}
