package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtensionProperties;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

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
         PointerBuffer requiredExtensions = createRequiredDeviceExtensions(stack);
         var deviceCreateInfo = StructureUtils.createDeviceInfo(stack, requiredExtensions, physicalDevice);

         PointerBuffer logicalDevicePointer = stack.mallocPointer(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateDevice(physicalDevice.getPhysicalDevice(), deviceCreateInfo, null, logicalDevicePointer), "Failed to create logical device!");
         this.device = new VkDevice(logicalDevicePointer.get(0), physicalDevice.getPhysicalDevice(), deviceCreateInfo);
      }
   }

   private PointerBuffer createRequiredDeviceExtensions(MemoryStack stack) {
      Set<String> deviceExtensions = getDeviceExtensions(stack);
      var extensionsList = PhysicalDevice.REQUIRED_EXTENSIONS.stream().map(stack::ASCII).toList();
      PointerBuffer requiredExtensions = stack.mallocPointer(extensionsList.size());
      extensionsList.forEach(requiredExtensions::put);
      requiredExtensions.flip();
      return requiredExtensions;
   }

   public void cleanup() {
      VK13.vkDestroyDevice(device, null);
      physicalDevice.cleanup();
   }

   public void waitIdle() {
      VK13.vkDeviceWaitIdle(device);
   }

   public VkDevice getDevice() {
      return device;
   }

   public PhysicalDevice getPhysicalDevice() {
      return physicalDevice;
   }

   private Set<String> getDeviceExtensions(MemoryStack stack) {
      IntBuffer numExtensionsBuf = stack.mallocInt(1);
      VK13.vkEnumerateDeviceExtensionProperties(physicalDevice.getPhysicalDevice(), (String) null, numExtensionsBuf, null);
      int numExtensions = numExtensionsBuf.get(0);

      VkExtensionProperties.Buffer extensionProperties = VkExtensionProperties.calloc(numExtensions, stack);
      VK13.vkEnumerateDeviceExtensionProperties(physicalDevice.getPhysicalDevice(), (String) null, numExtensionsBuf, extensionProperties);
      return extensionProperties.stream()
              .map(VkExtensionProperties::extensionNameString).collect(Collectors.toUnmodifiableSet());
   }
}
