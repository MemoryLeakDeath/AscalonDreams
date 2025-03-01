package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;

import java.util.List;

public class VulkanRenderer {
   private final VulkanRenderInstance instance;
   private VulkanWindow window;
   private final LogicalDevice device;
   private final VulkanGraphicsQueue graphicsQueue;
   private final VulkanSurface surface;
   private final VulkanSwapChain swapChain;
   private final VulkanCommandPool commandPool;
   private final ForwardRenderer forwardRenderer;
   private final VulkanPresentationQueue presentationQueue;

   public VulkanRenderer(VulkanWindow window) {
      this.instance = new VulkanRenderInstance(false);
      this.window = window;
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
      this.presentationQueue = new VulkanPresentationQueue(device, surface, 0);
      this.swapChain = new VulkanSwapChain(device, surface, window, VulkanSwapChain.TRIPLE_BUFFERING, true, presentationQueue, List.of(graphicsQueue));
      this.commandPool = new VulkanCommandPool(device, graphicsQueue.getQueueFamilyIndex());
      this.forwardRenderer = new ForwardRenderer(swapChain, commandPool);
   }

   public void cleanup() {
      presentationQueue.waitIdle();
      graphicsQueue.waitIdle();
      device.waitIdle();
      forwardRenderer.cleanup();
      commandPool.cleanup();
      swapChain.cleanup();
      surface.cleanup();
      device.cleanup();
      instance.cleanup();
   }

   public void render() {
      forwardRenderer.waitForFence();
      int imageIndex = swapChain.aquireNextImage();
      if (imageIndex < 0) {
         return;
      }
      forwardRenderer.submit(graphicsQueue);
      swapChain.showImage(presentationQueue, imageIndex);
   }
}
