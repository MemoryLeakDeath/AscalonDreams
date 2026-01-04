package tv.memoryleakdeath.ascalondreams.device;

public class ComputeQueue extends BaseDeviceQueue {
   public ComputeQueue(LogicalDevice device, int queueIndex) {
      createQueue(device, device.getPhysicalDevice().getComputeQueueFamilyIndex(), queueIndex);
   }
}
