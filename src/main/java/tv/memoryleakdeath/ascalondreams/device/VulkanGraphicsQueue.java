package tv.memoryleakdeath.ascalondreams.device;

public class VulkanGraphicsQueue extends BaseDeviceQueue {
   private static VulkanGraphicsQueue vulkanGraphicsQueue;

   private VulkanGraphicsQueue() {
      LogicalDevice device = DeviceManager.getDevice();
      createQueue(device, device.getPhysicalDevice().getGraphicsQueueIndex(), 0);
   }

   public static VulkanGraphicsQueue getInstance() {
      if(vulkanGraphicsQueue == null) {
         vulkanGraphicsQueue = new VulkanGraphicsQueue();
      }
      return vulkanGraphicsQueue;
   }
}
