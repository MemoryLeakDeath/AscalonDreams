package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.apache.commons.collections4.IterableUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties2;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSurface;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.IntBuffer;
import java.util.List;
import java.util.Set;

public final class PhysicalDevice {
   private static final Logger logger = LoggerFactory.getLogger(PhysicalDevice.class);
   public static final Set<String> REQUIRED_EXTENSIONS = Set.of(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
   private VkExtensionProperties.Buffer deviceExtensions;
   private VkPhysicalDeviceMemoryProperties memoryProperties;
   private final VkPhysicalDevice physicalDevice;
   private VkPhysicalDeviceFeatures deviceFeatures;
   private VkPhysicalDeviceProperties2 deviceProperties;
   private VkQueueFamilyProperties.Buffer queueFamilyProperties;

   private PhysicalDevice(VkPhysicalDevice physicalDevice) {
      this.physicalDevice = physicalDevice;
      init();
   }

   private void init() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer intBuffer = stack.mallocInt(1);
         deviceProperties = VkPhysicalDeviceProperties2.calloc().sType$Default();
         VK13.vkGetPhysicalDeviceProperties2(physicalDevice, deviceProperties);

         VulkanUtils.failIfNeeded(VK13.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, intBuffer, null),
                 "Failed to get number of device extension properties!");
         deviceExtensions = VkExtensionProperties.calloc(intBuffer.get(0));
         VulkanUtils.failIfNeeded(VK13.vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, intBuffer, deviceExtensions),
                 "Failed to get device extension properties!");

         VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intBuffer, null);
         queueFamilyProperties = VkQueueFamilyProperties.calloc(intBuffer.get(0));
         VK13.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, intBuffer, queueFamilyProperties);
         deviceFeatures = VkPhysicalDeviceFeatures.calloc();

         VK13.vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);

         memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
         VK13.vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
         logger.debug("Created Physical Device with name: {}", getDeviceName());
      }
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
      return deviceProperties.properties().deviceNameString();
   }

   public boolean isGameReady() {
      return (hasGraphicsQueueFamily() && hasKHRSwapChainExtension() && hasDiscreteGPU());
   }

   private boolean hasGraphicsQueueFamily() {
      return queueFamilyProperties.stream()
              .anyMatch(p -> (p.queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) != 0);
   }

   private boolean hasKHRSwapChainExtension() {
      return deviceExtensions.stream()
              .anyMatch(e -> KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(e.extensionNameString()));
   }

   private boolean hasDiscreteGPU() {
      return (deviceProperties.properties().deviceType() == VK13.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU);
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

   public VkPhysicalDeviceProperties2 getDeviceProperties() {
      return deviceProperties;
   }

   public VkQueueFamilyProperties.Buffer getQueueFamilyProperties() {
      return queueFamilyProperties;
   }

   public int getGraphicsQueueIndex() {
      int index = IterableUtils.indexOf(queueFamilyProperties, p -> (p.queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) != 0);
      if (index < 0) {
         throw new RuntimeException("No graphics queue family found!");
      }
      return index;
   }

   public int getPresentationQueueIndex(VulkanSurface surface) {
      int numQueueFamilies = getQueueFamilyProperties().capacity();
      int foundIndex = -1;
      try(var stack = MemoryStack.stackPush()) {
         IntBuffer buf = stack.mallocInt(1);
         for(int i = 0; i < numQueueFamilies; i++) {
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface.getId(), buf);
            if(buf.get(0) == VK13.VK_TRUE) {
               foundIndex = i;
               break;
            }
         }
      }
      if(foundIndex < 0) {
         throw new RuntimeException("Failed to get Presentation Queue family index");
      }
      return foundIndex;
   }
}
