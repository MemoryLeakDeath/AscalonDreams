package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.apache.commons.exec.OS;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Set;
import java.util.stream.Collectors;

public class LogicalDevice {
   private VkDevice device;
   private PhysicalDevice physicalDevice;

   public LogicalDevice(PhysicalDevice physicalDevice) {
      this.physicalDevice = physicalDevice;
      try (MemoryStack stack = MemoryStack.stackPush()) {

         // required extensions
         Set<String> deviceExtensions = getDeviceExtensions(stack);
         boolean usePortability = (deviceExtensions.contains(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME) && OS.isFamilyMac());
         int numExtensions = usePortability ? 2 : 1;
         PointerBuffer requiredExtensions = stack.mallocPointer(numExtensions);
         requiredExtensions.put(stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
         if (usePortability) {
            requiredExtensions.put(stack.ASCII(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME));
         }
         requiredExtensions.flip();

         // required features
         VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);

         // turn on all queue families
         VkQueueFamilyProperties.Buffer queueFamilyProperties = physicalDevice.getQueueFamilyProperties();
         int numQueueFamilies = queueFamilyProperties.capacity();
         VkDeviceQueueCreateInfo.Buffer queueCreationInfo = VkDeviceQueueCreateInfo.calloc(numQueueFamilies, stack);
         for (int i = 0; i < numQueueFamilies; i++) {
            FloatBuffer priorities = stack.callocFloat(queueFamilyProperties.get(i).queueCount());
            queueCreationInfo.get(i)
                    .sType(VK14.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(i)
                    .pQueuePriorities(priorities);
         }

         VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                 .ppEnabledExtensionNames(requiredExtensions)
                 .pEnabledFeatures(features)
                 .pQueueCreateInfos(queueCreationInfo);

         PointerBuffer logicalDevicePointer = stack.mallocPointer(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateDevice(physicalDevice.getPhysicalDevice(), deviceCreateInfo, null, logicalDevicePointer), "Failed to create logical device!");
         this.device = new VkDevice(logicalDevicePointer.get(0), physicalDevice.getPhysicalDevice(), deviceCreateInfo);
      }
   }

   public void cleanup() {
      VK14.vkDestroyDevice(device, null);
      physicalDevice.cleanup();
   }

   public void waitIdle() {
      VK14.vkDeviceWaitIdle(device);
   }

   public VkDevice getDevice() {
      return device;
   }

   public PhysicalDevice getPhysicalDevice() {
      return physicalDevice;
   }

   private Set<String> getDeviceExtensions(MemoryStack stack) {
      IntBuffer numExtensionsBuf = stack.mallocInt(1);
      VK14.vkEnumerateDeviceExtensionProperties(physicalDevice.getPhysicalDevice(), (String) null, numExtensionsBuf, null);
      int numExtensions = numExtensionsBuf.get(0);

      VkExtensionProperties.Buffer extensionProperties = VkExtensionProperties.calloc(numExtensions, stack);
      VK14.vkEnumerateDeviceExtensionProperties(physicalDevice.getPhysicalDevice(), (String) null, numExtensionsBuf, extensionProperties);
      return extensionProperties.stream()
              .map(VkExtensionProperties::extensionNameString).collect(Collectors.toUnmodifiableSet());
   }
}
