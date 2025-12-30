package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.IntBuffer;
import java.util.Set;
import java.util.stream.Collectors;

public class LogicalDevice {
   private static final Logger logger = LoggerFactory.getLogger(LogicalDevice.class);
   private VkDevice device;
   private PhysicalDevice physicalDevice;
   private boolean samplerAnisotropy;
   private final boolean depthClamp;

   public LogicalDevice(PhysicalDevice physicalDevice) {
      this.physicalDevice = physicalDevice;
      try (MemoryStack stack = MemoryStack.stackPush()) {

         // required extensions
         PointerBuffer requiredExtensions = createRequiredDeviceExtensions(stack);
         Object[] deviceCreateInfo = StructureUtils.createDeviceInfo(stack, requiredExtensions, physicalDevice);
         this.samplerAnisotropy = (boolean) deviceCreateInfo[1];
         this.depthClamp = (boolean) deviceCreateInfo[2];

         PointerBuffer logicalDevicePointer = stack.mallocPointer(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateDevice(physicalDevice.getPhysicalDevice(), (VkDeviceCreateInfo) deviceCreateInfo[0], null, logicalDevicePointer), "Failed to create logical device!");
         this.device = new VkDevice(logicalDevicePointer.get(0), physicalDevice.getPhysicalDevice(), (VkDeviceCreateInfo) deviceCreateInfo[0]);
      }
   }

   private PointerBuffer createRequiredDeviceExtensions(MemoryStack stack) {
      Set<String> deviceExtensions = getDeviceExtensions(stack);
      logger.debug("Found device extensions: {}", deviceExtensions);
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

      try(var extensionProperties = VkExtensionProperties.calloc(numExtensions)) {
         VK13.vkEnumerateDeviceExtensionProperties(physicalDevice.getPhysicalDevice(), (String) null, numExtensionsBuf, extensionProperties);
         return extensionProperties.stream()
                 .map(VkExtensionProperties::extensionNameString).collect(Collectors.toUnmodifiableSet());
      }
   }

   public boolean isSamplerAnisotropy() {
      return samplerAnisotropy;
   }

   public boolean isDepthClamp() {
      return depthClamp;
   }
}
