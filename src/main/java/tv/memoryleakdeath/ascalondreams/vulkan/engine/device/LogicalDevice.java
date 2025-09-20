package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class LogicalDevice {
   private static final Logger logger = LoggerFactory.getLogger(LogicalDevice.class);
   private VkDevice device;
   private PhysicalDevice physicalDevice;
   private boolean samplerAnsiotropy;

   public LogicalDevice(PhysicalDevice physicalDevice) {
      this.physicalDevice = physicalDevice;
      try (MemoryStack stack = MemoryStack.stackPush()) {

         // required extensions
         PointerBuffer requiredExtensions = StructureUtils.initRequiredExtensions(stack, physicalDevice.getPhysicalDevice());

         // required features
         VkPhysicalDeviceFeatures requestedFeatures = VkPhysicalDeviceFeatures.calloc(stack);
         VkPhysicalDeviceFeatures deviceSupportedFeatures = physicalDevice.getDeviceFeatures();
         if(deviceSupportedFeatures.samplerAnisotropy()) {
            this.samplerAnsiotropy = true;
            requestedFeatures.samplerAnisotropy(samplerAnsiotropy);
         }

         // turn on all queue families
         VkQueueFamilyProperties.Buffer queueFamilyProperties = physicalDevice.getQueueFamilyProperties();
         VkDeviceQueueCreateInfo.Buffer queueCreationInfo = StructureUtils.initQueueFamilies(stack, queueFamilyProperties);
         this.device = StructureUtils.createDevice(stack, requiredExtensions, requestedFeatures, queueCreationInfo, physicalDevice.getPhysicalDevice());
         logger.debug("created logical device with address: {}",device.address());
      }
   }

   public void cleanup() {
      logger.debug("destroying logical device with address: {}", device.address());
      VK14.vkDestroyDevice(device, null);
      physicalDevice.cleanup();
   }

   public void waitIdle() {
      logger.debug("waiting for device idle: {}", device.address());
      VK14.vkDeviceWaitIdle(device);
   }

   public VkDevice getDevice() {
      return device;
   }

   public PhysicalDevice getPhysicalDevice() {
      return physicalDevice;
   }

   public boolean isSamplerAnsiotropy() {
      return samplerAnsiotropy;
   }
}
