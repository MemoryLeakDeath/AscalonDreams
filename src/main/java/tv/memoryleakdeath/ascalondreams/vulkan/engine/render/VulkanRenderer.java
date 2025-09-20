package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PipelineCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

import java.util.ArrayList;
import java.util.List;

public class VulkanRenderer {
   private static final Logger logger = LoggerFactory.getLogger(VulkanRenderer.class);
   private final VulkanRenderInstance instance;
   private VulkanWindow window;
   private VulkanScene scene;
   private VulkanSwapChain swapChain;
   private final LogicalDevice device;
   private final VulkanGraphicsQueue graphicsQueue;
   private final VulkanSurface surface;
   private final VulkanCommandPool commandPool;
   private final ForwardRenderer forwardRenderer;
   private final VulkanPresentationQueue presentationQueue;
   private final PipelineCache pipelineCache;
   private final List<VulkanModel> vulkanModels = new ArrayList<>();

   public VulkanRenderer(VulkanWindow window, VulkanScene scene) {
      this.instance = new VulkanRenderInstance(false);
      this.window = window;
      this.scene = scene;
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
      this.presentationQueue = new VulkanPresentationQueue(device, surface, 0);
      this.swapChain = new VulkanSwapChain(device, surface, window, VulkanSwapChain.TRIPLE_BUFFERING, true, presentationQueue, List.of(graphicsQueue));
      this.commandPool = new VulkanCommandPool(device, graphicsQueue.getQueueFamilyIndex());
      this.pipelineCache = new PipelineCache(device);
      this.forwardRenderer = new ForwardRenderer(swapChain, commandPool, pipelineCache, scene, surface);
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
      if (window.getWidth() <= 0 && window.getHeight() <= 0) {
         return;
      }
      forwardRenderer.waitForFence();
      int imageIndex = 0;
      if (window.isResized() || (imageIndex = swapChain.aquireNextImage()) < 0) {
         logger.debug("Window Resized!! Image Index: {}", imageIndex);
         window.resetResized();
         resize();
         scene.getProjection().resize(window.getWidth(), window.getHeight());
         imageIndex = swapChain.aquireNextImage();
      }
      forwardRenderer.recordCommandBuffer(vulkanModels);
      forwardRenderer.submit(graphicsQueue);
      if(swapChain.showImage(presentationQueue, imageIndex)) {
         window.setResized(true);
      }
   }

   private void resize() {
      device.waitIdle();
      graphicsQueue.waitIdle();
      swapChain.cleanup();
      this.swapChain = new VulkanSwapChain(device, surface, window,
              3, true, presentationQueue, List.of(graphicsQueue));
      forwardRenderer.resize(swapChain);
   }
}
