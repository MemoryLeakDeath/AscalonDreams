package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

public record VulkanDeviceAndProperties(VkPhysicalDevice physicalDevice, VkPhysicalDeviceProperties deviceProperties) {

}
