package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PipelineCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;

import java.util.ArrayList;
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
   private final PipelineCache pipelineCache;
   private final List<VulkanModel> vulkanModels = new ArrayList<>();

   public VulkanRenderer(VulkanWindow window) {
      this.instance = new VulkanRenderInstance(false);
      this.window = window;
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
      this.presentationQueue = new VulkanPresentationQueue(device, surface, 0);
      this.swapChain = new VulkanSwapChain(device, surface, window, VulkanSwapChain.TRIPLE_BUFFERING, true, presentationQueue, List.of(graphicsQueue));
      this.commandPool = new VulkanCommandPool(device, graphicsQueue.getQueueFamilyIndex());
      this.pipelineCache = new PipelineCache(device);
      this.forwardRenderer = new ForwardRenderer(swapChain, commandPool, pipelineCache);
   }

   public void cleanup() {
      presentationQueue.waitIdle();
      graphicsQueue.waitIdle();
      device.waitIdle();
      vulkanModels.forEach(VulkanModel::cleanup);
      pipelineCache.cleanup();
      forwardRenderer.cleanup();
      commandPool.cleanup();
      swapChain.cleanup();
      surface.cleanup();
      device.cleanup();
      instance.cleanup();
   }

   public void loadModels(List<VulkanModel> models) {
      for (VulkanModel model : models) {
         model.prepareModel(commandPool, graphicsQueue);
         vulkanModels.add(model);
      }
   }

   public void render() {
      forwardRenderer.waitForFence();
      int imageIndex = swapChain.aquireNextImage();
      if (imageIndex < 0) {
         return;
      }
      forwardRenderer.recordCommandBuffer(vulkanModels);
      forwardRenderer.submit(graphicsQueue);
      swapChain.showImage(presentationQueue, imageIndex);
   }
}
