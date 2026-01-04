package tv.memoryleakdeath.ascalondreams.device;

public class VulkanGraphicsQueue extends BaseDeviceQueue {
   public VulkanGraphicsQueue(LogicalDevice device, int queueIndex) {
      createQueue(device, device.getPhysicalDevice().getGraphicsQueueIndex(), queueIndex);
   }
}
