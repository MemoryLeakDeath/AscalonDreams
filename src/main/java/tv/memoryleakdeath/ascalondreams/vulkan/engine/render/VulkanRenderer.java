package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.gui.GuiRender;
import tv.memoryleakdeath.ascalondreams.gui.GuiTexture;
import tv.memoryleakdeath.ascalondreams.lighting.LightingRenderer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Semaphore;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.MaterialCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.ModelCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.TextureCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ConvertedModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.postprocess.PostProcessingRenderer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.swapchain.SwapChainRender;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.util.ArrayList;
import java.util.List;

public class VulkanRenderer {
   private static final Logger logger = LoggerFactory.getLogger(VulkanRenderer.class);
   private final VulkanRenderInstance instance;
   private static final boolean VSYNC = true;
   private static final int BUFFERING_SETUP = 3;
   private VulkanWindow window;
   private final LogicalDevice device;
   private VulkanSurface surface;
   private VulkanSwapChain swapChain;

   private final List<CommandBuffer> commandBuffers = new ArrayList<>();
   private final List<CommandPool> commandPools = new ArrayList<>();
   private final List<Fence> fences = new ArrayList<>();
   private final VulkanGraphicsQueue graphicsQueue;
   private final List<Semaphore> presentationCompleteSemaphores = new ArrayList<>();
   private final LightingRenderer lightingRenderer;
   private final ModelCache modelCache;
   private final PostProcessingRenderer postProcessingRenderer;
   private final MaterialCache materialCache;
   private final TextureCache textureCache;
   private final VulkanPresentationQueue presentationQueue;
   private final List<Semaphore> renderingCompleteSemaphores = new ArrayList<>();
   private final GuiRender guiRender;
   private final SceneRenderer sceneRenderer;
   private final SwapChainRender swapChainRender;
   private final PipelineCache pipelineCache;
   private int currentFrame = 0;
   private boolean resize = false;
   private VulkanScene currentScene;
   private final DescriptorAllocator descriptorAllocator;
   private final MemoryAllocationUtil memoryAllocationUtil;



   public VulkanRenderer(VulkanWindow window, VulkanScene scene) {
      this.window = window;
      this.currentScene = scene;
      this.instance = new VulkanRenderInstance(true);
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.swapChain = new VulkanSwapChain(device, surface, window, BUFFERING_SETUP, VSYNC);
      this.pipelineCache = new PipelineCache(device);
      this.descriptorAllocator = new DescriptorAllocator(device.getPhysicalDevice(), device);
      this.memoryAllocationUtil = new MemoryAllocationUtil(instance, device);

      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
      this.presentationQueue = new VulkanPresentationQueue(device, surface, 0);

      for(int i = 0; i < VulkanConstants.MAX_IN_FLIGHT; i++) {
         CommandPool pool = new CommandPool(device, graphicsQueue.getQueueFamilyIndex(), false);
         commandPools.add(pool);
         commandBuffers.add(new CommandBuffer(device, pool, true, true));
         fences.add(new Fence(device, true));
         presentationCompleteSemaphores.add(new Semaphore(device));
      }

      for(int i = 0; i < swapChain.getNumImages(); i++) {
         renderingCompleteSemaphores.add(new Semaphore(device));
      }

      this.sceneRenderer = new SceneRenderer(swapChain, pipelineCache, device, descriptorAllocator, scene, memoryAllocationUtil);
      this.lightingRenderer = new LightingRenderer(device, descriptorAllocator, memoryAllocationUtil, swapChain, pipelineCache, sceneRenderer.getMaterialAttachments().getAllAttachments());
      this.postProcessingRenderer = new PostProcessingRenderer(device, memoryAllocationUtil, descriptorAllocator, swapChain, pipelineCache, lightingRenderer.getAttachmentColor());
      this.guiRender = new GuiRender(device, descriptorAllocator, pipelineCache, swapChain, memoryAllocationUtil, graphicsQueue, postProcessingRenderer.getColorAttachment());
      this.swapChainRender = new SwapChainRender(device, descriptorAllocator, swapChain, surface, pipelineCache, postProcessingRenderer.getColorAttachment());
      this.modelCache = ModelCache.getInstance();
      this.textureCache = new TextureCache();
      this.materialCache = MaterialCache.getInstance();
   }

   public void cleanup() {
      device.waitIdle();

      sceneRenderer.cleanup(device, memoryAllocationUtil);
      postProcessingRenderer.cleanup(device, memoryAllocationUtil);
      lightingRenderer.cleanup(device, memoryAllocationUtil);
      guiRender.cleanup(device, memoryAllocationUtil);
      swapChainRender.cleanup(device);
      modelCache.cleanup(device, memoryAllocationUtil);
      textureCache.cleanup(device, memoryAllocationUtil);
      materialCache.cleanup(device, memoryAllocationUtil);

      renderingCompleteSemaphores.forEach(s -> s.cleanup(device));
      presentationCompleteSemaphores.forEach(s -> s.cleanup(device));
      fences.forEach(f -> f.cleanup(device));

      for(int i = 0; i < commandPools.size(); i++) {
         commandBuffers.get(i).cleanup(device, commandPools.get(i));
         commandPools.get(i).cleanup(device);
      }

      memoryAllocationUtil.cleanup();
      descriptorAllocator.cleanup(device);
      pipelineCache.cleanup(device);
      swapChain.cleanup();
      surface.cleanup();
      device.cleanup();
      instance.cleanup();
   }

   public void initModels(ConvertedModel convertedModel, List<GuiTexture> guiTextures) {
      logger.debug("Loading {} materials", convertedModel.getMaterials().size());
      materialCache.loadMaterials(device, memoryAllocationUtil, convertedModel.getMaterials(), textureCache, commandPools.getFirst(), graphicsQueue);
      logger.debug("Loaded materials.");

      if(guiTextures != null) {
         guiTextures.forEach(t -> textureCache.addTexture(device, memoryAllocationUtil, t.texturePath(), t.texturePath(),
                 VK13.VK_FORMAT_R8G8B8A8_SRGB));
      }

      logger.debug("Transitioning textures....");
      textureCache.recordTextureTransitions(device, memoryAllocationUtil, commandPools.getFirst(), graphicsQueue);
      logger.debug("textures transitioned.");

      logger.debug("Loading model...");
      VulkanModel model = new VulkanModel(convertedModel.getId());
      model.addMeshes(device, memoryAllocationUtil, convertedModel.getMeshData());

      modelCache.loadModels(device, memoryAllocationUtil, List.of(model), commandPools.getFirst(), graphicsQueue);
      logger.debug("Models loaded!");

      sceneRenderer.loadMaterials(device, descriptorAllocator, materialCache, textureCache);
      guiRender.loadTextures(device, descriptorAllocator, guiTextures, textureCache);
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

      sceneRenderer.render(commandBuffer, scene, descriptorAllocator, currentFrame, device, memoryAllocationUtil);
      lightingRenderer.render(descriptorAllocator, commandBuffer, sceneRenderer.getMaterialAttachments());
      postProcessingRenderer.render(swapChain, descriptorAllocator, commandBuffer, lightingRenderer.getAttachmentColor());
      guiRender.render(device, descriptorAllocator, memoryAllocationUtil, commandBuffer, currentFrame, postProcessingRenderer.getColorAttachment());

      int imageIndex;
      if(resize || (imageIndex = swapChain.acquireNextImage(device, presentationCompleteSemaphores.get(currentFrame))) < 0) {
         resize(window.getWidth(), window.getHeight(), scene);
         return;
      }

      swapChainRender.render(swapChain, descriptorAllocator, commandBuffer, postProcessingRenderer.getColorAttachment(), imageIndex);

      stopRecording(commandBuffer);
      submit(commandBuffer, imageIndex);
      resize = swapChain.presentImage(presentationQueue, renderingCompleteSemaphores.get(imageIndex), imageIndex);

      currentFrame = (currentFrame + 1) % VulkanConstants.MAX_IN_FLIGHT;
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

      for(int i = 0; i < VulkanConstants.MAX_IN_FLIGHT; i++) {
         presentationCompleteSemaphores.add(new Semaphore(device));
      }
      for(int i = 0; i < swapChain.getNumImages(); i++) {
         renderingCompleteSemaphores.add(new Semaphore(device));
      }

      VkExtent2D extent = swapChain.getSwapChainExtent();
      scene.getProjection().resize(extent.width(), extent.height());
      sceneRenderer.resize(device, memoryAllocationUtil, swapChain, scene);
      lightingRenderer.resize(device, memoryAllocationUtil, swapChain, descriptorAllocator, sceneRenderer.getMaterialAttachments().getAllAttachments());
      postProcessingRenderer.resize(device, memoryAllocationUtil, swapChain, descriptorAllocator, lightingRenderer.getAttachmentColor());
      guiRender.resize(device, swapChain, postProcessingRenderer.getColorAttachment());
      swapChainRender.resize(device, swapChain, descriptorAllocator, postProcessingRenderer.getColorAttachment());
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
