package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanEngineConfiguration;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.*;
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
   private VulkanSurface surface;
   private List<VulkanCommandPool> commandPools = new ArrayList<>();
   private List<VulkanCommandBuffer> commandBuffers = new ArrayList<>();
   private List<Fence> fences = new ArrayList<>();
   private List<Semaphore> imageAquiredSemaphores = new ArrayList<>();
   private List<Semaphore> renderingCompleteSemaphores = new ArrayList<>();
   private boolean resize = false;
   private final VulkanSceneRenderer forwardRenderer;
   private final VulkanPresentationQueue presentationQueue;
   private final PipelineCache pipelineCache;
   private final List<VulkanModel> vulkanModels = new ArrayList<>();
   private final DescriptorAllocator descriptorAllocator;

   public VulkanRenderer(VulkanWindow window, VulkanScene scene) {
      this.instance = new VulkanRenderInstance(false);
      this.window = window;
      this.scene = scene;
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
      this.presentationQueue = new VulkanPresentationQueue(device, surface, 0);
      this.swapChain = new VulkanSwapChain(device, surface, window, VulkanSwapChain.TRIPLE_BUFFERING, true, presentationQueue, List.of(graphicsQueue));
      this.pipelineCache = new PipelineCache(device);
      this.descriptorAllocator = new DescriptorAllocator(device);
      initBuffersFencesAndSemaphores(device, graphicsQueue, swapChain.getNumImages());

      this.forwardRenderer = new VulkanSceneRenderer(swapChain, commandPools, pipelineCache, scene, surface);
   }

   private void initBuffersFencesAndSemaphores(LogicalDevice device, VulkanGraphicsQueue graphicsQueue, int numSwapChainImages) {
      for(int i = 0; i < VulkanEngineConfiguration.getInstance().getMaxInFlightCommandBuffers(); i++) {
         VulkanCommandPool pool = new VulkanCommandPool(device, graphicsQueue.getQueueFamilyIndex());
         commandPools.add(pool);
         commandBuffers.add(new VulkanCommandBuffer(pool, true, true));
         fences.add(new Fence(device, true));
         imageAquiredSemaphores.add(new Semaphore(device));
      }

      for(int i = 0; i < numSwapChainImages; i++) {
         renderingCompleteSemaphores.add(new Semaphore(device));
      }
   }

   public void cleanup() {
      presentationQueue.waitIdle();
      graphicsQueue.waitIdle();
      device.waitIdle();
      vulkanModels.forEach(VulkanModel::cleanup);
      descriptorAllocator.cleanup(device);
      pipelineCache.cleanup();
      forwardRenderer.cleanup();
      commandPools.forEach(VulkanCommandPool::cleanup);
      swapChain.cleanup();
      surface.cleanup();
      device.cleanup();
      instance.cleanup();
   }

   public void loadModels(List<VulkanModel> models) {
      for (VulkanModel model : models) {
         model.prepareModel(commandPools, graphicsQueue);
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
      surface.cleanup();
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.swapChain = new VulkanSwapChain(device, surface, window,
              3, true, presentationQueue, List.of(graphicsQueue));
      forwardRenderer.resize(swapChain);
   }
}
