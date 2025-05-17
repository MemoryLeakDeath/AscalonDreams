package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class LogicalDevice {
   private VkDevice device;
   private PhysicalDevice physicalDevice;

   public LogicalDevice(PhysicalDevice physicalDevice) {
      this.physicalDevice = physicalDevice;
      try (MemoryStack stack = MemoryStack.stackPush()) {

         // required extensions
         PointerBuffer requiredExtensions = StructureUtils.initRequiredExtensions(stack, physicalDevice.getPhysicalDevice());

         // required features
         VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);

         // turn on all queue families
         VkQueueFamilyProperties.Buffer queueFamilyProperties = physicalDevice.getQueueFamilyProperties();
         VkDeviceQueueCreateInfo.Buffer queueCreationInfo = StructureUtils.initQueueFamilies(stack, queueFamilyProperties);
         this.device = StructureUtils.createDevice(stack, requiredExtensions, features, queueCreationInfo, physicalDevice.getPhysicalDevice());
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
}
