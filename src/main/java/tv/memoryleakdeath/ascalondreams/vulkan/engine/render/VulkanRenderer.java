package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Semaphore;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.ModelCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

import java.util.ArrayList;
import java.util.List;

public class VulkanRenderer {
   private static final Logger logger = LoggerFactory.getLogger(VulkanRenderer.class);
   private final VulkanRenderInstance instance;
   private static final boolean VSYNC = true;
   private static final int BUFFERING_SETUP = 3;
   private static final int MAX_IN_FLIGHT = 2;
   private VulkanWindow window;
   private final LogicalDevice device;
   private VulkanSurface surface;
   private VulkanSwapChain swapChain;

   private final List<CommandBuffer> commandBuffers = new ArrayList<>();
   private final List<CommandPool> commandPools = new ArrayList<>();
   private final List<Fence> fences = new ArrayList<>();
   private final VulkanGraphicsQueue graphicsQueue;
   private final List<Semaphore> presentationCompleteSemaphores = new ArrayList<>();
   private final ModelCache modelCache;
   private final VulkanPresentationQueue presentationQueue;
   private final List<Semaphore> renderingCompleteSemaphores = new ArrayList<>();
   private final SceneRenderer sceneRenderer;
   private final PipelineCache pipelineCache;
   private int currentFrame = 0;
   private boolean resize = false;



   public VulkanRenderer(VulkanWindow window) {
      this.window = window;
      this.instance = new VulkanRenderInstance(true);
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.swapChain = new VulkanSwapChain(device, surface, window, BUFFERING_SETUP, VSYNC);
      this.pipelineCache = new PipelineCache(device);

      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
      this.presentationQueue = new VulkanPresentationQueue(device, surface, 0);

      for(int i = 0; i < MAX_IN_FLIGHT; i++) {
         CommandPool pool = new CommandPool(device, graphicsQueue.getQueueFamilyIndex(), false);
         commandPools.add(pool);
         commandBuffers.add(new CommandBuffer(device, pool, true, true));
         fences.add(new Fence(device, true));
         presentationCompleteSemaphores.add(new Semaphore(device));
      }

      for(int i = 0; i < swapChain.getNumImages(); i++) {
         renderingCompleteSemaphores.add(new Semaphore(device));
      }

      this.sceneRenderer = new SceneRenderer(swapChain, surface, pipelineCache, device);
      this.modelCache = new ModelCache();
   }

   public void cleanup() {
      device.waitIdle();

      sceneRenderer.cleanup(device);
      modelCache.cleanup(device);

      renderingCompleteSemaphores.forEach(s -> s.cleanup(device));
      presentationCompleteSemaphores.forEach(s -> s.cleanup(device));
      fences.forEach(f -> f.cleanup(device));

      for(int i = 0; i < commandPools.size(); i++) {
         commandBuffers.get(i).cleanup(device, commandPools.get(i));
         commandPools.get(i).cleanup(device);
      }

      swapChain.cleanup();
      surface.cleanup();
      device.cleanup();
      instance.cleanup();
   }

   public void initModels(List<VulkanModel> modelList) {
      logger.debug("Loading {} models", modelList.size());
      modelCache.loadModels(device, modelList, commandPools.getFirst(), graphicsQueue);
      logger.debug("Models loaded!");
   }

   private void startRecording(CommandPool pool, CommandBuffer buf) {
      pool.reset(device);
      buf.beginRecording();
   }

   private void stopRecording(CommandBuffer buf) {
      buf.endRecording();
   }

   public void render(VulkanScene scene) {
      waitForFence();
      var commandPool = commandPools.get(currentFrame);
      var commandBuffer = commandBuffers.get(currentFrame);

      startRecording(commandPool, commandBuffer);

      int imageIndex;
      if(resize || (imageIndex = swapChain.acquireNextImage(device, presentationCompleteSemaphores.get(currentFrame))) < 0) {
         resize(window.getWidth(), window.getHeight(), scene);
         return;
      }
      sceneRenderer.render(swapChain, commandBuffer, modelCache, imageIndex, scene);

      stopRecording(commandBuffer);
      submit(commandBuffer, imageIndex);
      resize = swapChain.presentImage(presentationQueue, renderingCompleteSemaphores.get(imageIndex), imageIndex);

      currentFrame = (currentFrame + 1) % MAX_IN_FLIGHT;
   }

   private void resize(int width, int height, VulkanScene scene) {
      if(width == 0 && height == 0) {
         return;
      }
      resize = false;
      device.waitIdle();
      doResizeCleanup(scene);
   }

   private void doResizeCleanup(VulkanScene scene) {
      swapChain.cleanup();
      surface.cleanup();
      surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      swapChain = new VulkanSwapChain(device, surface, window, BUFFERING_SETUP, VSYNC);

      renderingCompleteSemaphores.forEach(s -> s.cleanup(device));
      presentationCompleteSemaphores.forEach(s -> s.cleanup(device));
      renderingCompleteSemaphores.clear();
      presentationCompleteSemaphores.clear();

      for(int i = 0; i < MAX_IN_FLIGHT; i++) {
         presentationCompleteSemaphores.add(new Semaphore(device));
      }
      for(int i = 0; i < swapChain.getNumImages(); i++) {
         renderingCompleteSemaphores.add(new Semaphore(device));
      }

      VkExtent2D extent = swapChain.getSwapChainExtent();
      scene.getProjection().resize(extent.width(), extent.height());
      sceneRenderer.resize(device, swapChain);
   }

   private void submit(CommandBuffer buf, int imageIndex) {
      try(var stack = MemoryStack.stackPush()) {
         var fence = fences.get(currentFrame);
         fence.reset(device);
         var commands = VkCommandBufferSubmitInfo.calloc(1, stack)
                 .sType$Default()
                 .commandBuffer(buf.getCommandBuffer());
         var waitSemaphores = VkSemaphoreSubmitInfo.calloc(1, stack)
                 .sType$Default()
                 .stageMask(VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
                 .semaphore(presentationCompleteSemaphores.get(currentFrame).getId());
         var signalSemaphores = VkSemaphoreSubmitInfo.calloc(1, stack)
                 .sType$Default()
                 .stageMask(VK13.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT)
                 .semaphore(renderingCompleteSemaphores.get(imageIndex).getId());
         graphicsQueue.submit(commands, waitSemaphores, signalSemaphores, fence);
      }
   }

   private void waitForFence() {
      var fence = fences.get(currentFrame);
      fence.fenceWait(device);
   }

   public LogicalDevice getDevice() {
      return device;
   }
}
