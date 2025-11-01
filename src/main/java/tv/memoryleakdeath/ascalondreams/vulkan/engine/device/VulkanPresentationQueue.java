package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSurface;

public class VulkanPresentationQueue extends BaseDeviceQueue {
   public VulkanPresentationQueue(LogicalDevice device, VulkanSurface surface, int queueIndex) {
      createQueue(device, device.getPhysicalDevice().getPresentationQueueIndex(surface), queueIndex);
   }
}
