package tv.memoryleakdeath.ascalondreams.device;

public class DeviceManager {
   private static LogicalDevice device;

   private DeviceManager() {
   }

   public static LogicalDevice createDevice(PhysicalDevice physicalDevice) {
      device = new LogicalDevice(physicalDevice);
      return device;
   }

   public static LogicalDevice getDevice() {
      return device;
   }
}
