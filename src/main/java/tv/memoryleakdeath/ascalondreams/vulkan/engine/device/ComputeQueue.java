package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

public class ComputeQueue extends BaseDeviceQueue {
   public ComputeQueue(LogicalDevice device, int queueIndex) {
      createQueue(device, device.getPhysicalDevice().getComputeQueueFamilyIndex(), queueIndex);
   }
}
