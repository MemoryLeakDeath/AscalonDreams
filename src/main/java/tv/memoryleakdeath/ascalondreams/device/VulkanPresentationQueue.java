package tv.memoryleakdeath.ascalondreams.device;

import tv.memoryleakdeath.ascalondreams.render.VulkanSurface;

public class VulkanPresentationQueue extends BaseDeviceQueue {
   public VulkanPresentationQueue(LogicalDevice device, VulkanSurface surface, int queueIndex) {
      createQueue(device, device.getPhysicalDevice().getPresentationQueueIndex(surface), queueIndex);
   }
}
