package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsQueue;

public class VulkanRenderer {
   private final VulkanRenderInstance instance;
   private VulkanWindow window;
   private final LogicalDevice device;
   private final VulkanGraphicsQueue graphicsQueue;
   private final VulkanSurface surface;

   public VulkanRenderer(VulkanWindow window) {
      this.instance = new VulkanRenderInstance(false);
      this.window = window;
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
   }

   public void cleanup() {
      surface.cleanup();
      device.cleanup();
      instance.cleanup();
   }

   public void render() {
      // todo implement
   }
}
