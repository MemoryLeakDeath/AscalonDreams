package tv.memoryleakdeath.ascalondreams.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.animations.AnimationCache;
import tv.memoryleakdeath.ascalondreams.animations.AnimationRenderer;
import tv.memoryleakdeath.ascalondreams.buffers.GlobalBuffers;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.gui.GuiRender;
import tv.memoryleakdeath.ascalondreams.gui.GuiTexture;
import tv.memoryleakdeath.ascalondreams.lighting.LightingRenderer;
import tv.memoryleakdeath.ascalondreams.shadow.ShadowRenderer;
import tv.memoryleakdeath.ascalondreams.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.device.Fence;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.device.Semaphore;
import tv.memoryleakdeath.ascalondreams.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.cache.MaterialCache;
import tv.memoryleakdeath.ascalondreams.cache.ModelCache;
import tv.memoryleakdeath.ascalondreams.cache.TextureCache;
import tv.memoryleakdeath.ascalondreams.model.VulkanMaterial;
import tv.memoryleakdeath.ascalondreams.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.model.conversion.ConvertedModel;
import tv.memoryleakdeath.ascalondreams.postprocess.PostProcessingRenderer;
import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.swapchain.SwapChainRender;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;

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

   private final AnimationRenderer animationRenderer;
   private final AnimationCache animationCache = AnimationCache.getInstance();
   private final List<CommandBuffer> commandBuffers = new ArrayList<>();
   private final List<CommandPool> commandPools = new ArrayList<>();
   private final List<Fence> fences = new ArrayList<>();
   private final GlobalBuffers globalBuffers = new GlobalBuffers();
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
   private final ShadowRenderer shadowRenderer;
   private final SwapChainRender swapChainRender;
   private final PipelineCache pipelineCache;
   private boolean resize = false;
   private VulkanScene currentScene;
   private final DescriptorAllocator descriptorAllocator;
   private final MemoryAllocationUtil memoryAllocationUtil;



   public VulkanRenderer(VulkanWindow window, VulkanScene scene) {
      this.window = window;
      this.currentScene = scene;
      this.instance = new VulkanRenderInstance(true);
      this.device = DeviceManager.createDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = VulkanSurface.createInstance(device.getPhysicalDevice(), window.getHandle());
      this.swapChain = VulkanSwapChain.createInstance(window, BUFFERING_SETUP, VSYNC);
      this.pipelineCache = PipelineCache.getInstance();
      this.descriptorAllocator = DescriptorAllocator.getInstance();
      this.memoryAllocationUtil = MemoryAllocationUtil.createInstance(instance);

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

      RenderChain.initChain();
      this.sceneRenderer = new SceneRenderer(swapChain, pipelineCache, device, descriptorAllocator, currentScene, memoryAllocationUtil);
      this.shadowRenderer = new ShadowRenderer(device, memoryAllocationUtil, descriptorAllocator, pipelineCache);
      List<Attachment> shadowAttachments = new ArrayList<>(sceneRenderer.getMaterialAttachments().getColorAttachments());
      shadowAttachments.add(shadowRenderer.getColorAttachment());
      this.lightingRenderer = new LightingRenderer(device, descriptorAllocator, memoryAllocationUtil, swapChain, pipelineCache, shadowAttachments);
      this.postProcessingRenderer = new PostProcessingRenderer(device, memoryAllocationUtil, descriptorAllocator, swapChain, pipelineCache, lightingRenderer.getAttachmentColor());
      this.guiRender = new GuiRender(device, descriptorAllocator, pipelineCache, swapChain, memoryAllocationUtil, graphicsQueue, postProcessingRenderer.getColorAttachment());
      this.swapChainRender = new SwapChainRender(device, descriptorAllocator, swapChain, surface, pipelineCache, postProcessingRenderer.getColorAttachment());
      this.animationRenderer = new AnimationRenderer(device, pipelineCache);
      this.modelCache = ModelCache.getInstance();
      this.textureCache = TextureCache.getInstance();
      this.materialCache = MaterialCache.getInstance();
   }

   public void cleanup() {
      device.waitIdle();

      globalBuffers.cleanup(device, memoryAllocationUtil);
      animationRenderer.cleanup(device);
      sceneRenderer.cleanup(device, memoryAllocationUtil);
      postProcessingRenderer.cleanup(device, memoryAllocationUtil);
      lightingRenderer.cleanup(device, memoryAllocationUtil);
      guiRender.cleanup(device, memoryAllocationUtil);
      shadowRenderer.cleanup(device, memoryAllocationUtil);
      swapChainRender.cleanup(device);
      modelCache.cleanup(device, memoryAllocationUtil);
      textureCache.cleanup(device, memoryAllocationUtil);
      materialCache.cleanup(device, memoryAllocationUtil);
      animationCache.cleanup(device, memoryAllocationUtil);

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

   public void initModels(List<ConvertedModel> convertedModels, List<GuiTexture> guiTextures) {
      if(guiTextures != null) {
         guiTextures.forEach(t -> textureCache.addTexture(device, memoryAllocationUtil, t.texturePath(), t.texturePath(),
                 VK13.VK_FORMAT_R8G8B8A8_SRGB));
      }
      List<VulkanMaterial> allMaterials = convertedModels.stream().flatMap(m -> m.getMaterials().stream()).toList();
      logger.debug("Loading {} materials", allMaterials.size());
      materialCache.loadMaterials(device, memoryAllocationUtil, allMaterials, textureCache, commandPools.getFirst(), graphicsQueue);
      logger.debug("Loaded materials.");
      logger.debug("Transitioning textures....");
      textureCache.recordTextureTransitions(device, memoryAllocationUtil, commandPools.getFirst(), graphicsQueue);
      logger.debug("textures transitioned.");

      List<VulkanModel> models = new ArrayList<>();
      for(ConvertedModel convertedModel : convertedModels) {
         logger.debug("Loading model: {}", convertedModel.getId());
         VulkanModel model = new VulkanModel(convertedModel.getId());
         if(convertedModel.getAnimations() != null) {
            model.addAnimations(device, memoryAllocationUtil, convertedModel.getAnimations());
         }
         model.addMeshes(device, memoryAllocationUtil, convertedModel.getMeshData(), convertedModel.getAnimationMeshData());
         models.add(model);
      }
      modelCache.loadModels(device, memoryAllocationUtil, models, commandPools.getFirst(), graphicsQueue);
      logger.debug("Models loaded!");

      sceneRenderer.loadMaterials(device, descriptorAllocator, materialCache, textureCache);
      shadowRenderer.loadMaterials(device, descriptorAllocator, materialCache, textureCache);
      guiRender.loadTextures(device, descriptorAllocator, guiTextures, textureCache);
      animationCache.loadAnimations(device, memoryAllocationUtil, currentScene.getEntities(), modelCache);
      animationRenderer.loadModels(modelCache);
   }

   private void startRecording(CommandPool pool, CommandBuffer buf) {
      pool.reset(device);
      buf.beginRecording();
   }

   private void stopRecording(CommandBuffer buf) {
      buf.endRecording();
   }

   public void render(VulkanScene scene, int currentFrame) {
      waitForFence(currentFrame);
      var commandPool = commandPools.get(currentFrame);
      var commandBuffer = commandBuffers.get(currentFrame);

      animationRenderer.render(device, currentScene, modelCache);

      startRecording(commandPool, commandBuffer);

      sceneRenderer.render(commandBuffer, scene, descriptorAllocator, currentFrame, device, memoryAllocationUtil, globalBuffers);
      shadowRenderer.render(device, memoryAllocationUtil, descriptorAllocator, scene, commandBuffer, modelCache, materialCache, currentFrame, globalBuffers);
      lightingRenderer.render(device, memoryAllocationUtil, descriptorAllocator, scene, commandBuffer, sceneRenderer.getMaterialAttachments(),
              shadowRenderer.getColorAttachment(), shadowRenderer.getCascadeShadows(currentFrame), currentFrame);
      postProcessingRenderer.render(swapChain, descriptorAllocator, commandBuffer, lightingRenderer.getAttachmentColor());
      guiRender.render(device, descriptorAllocator, memoryAllocationUtil, commandBuffer, currentFrame, postProcessingRenderer.getColorAttachment());

      int imageIndex;
      if(resize || (imageIndex = swapChain.acquireNextImage(device, presentationCompleteSemaphores.get(currentFrame))) < 0) {
         resize(window.getWidth(), window.getHeight(), scene);
         return;
      }

      swapChainRender.render(swapChain, descriptorAllocator, commandBuffer, postProcessingRenderer.getColorAttachment(), imageIndex);

      stopRecording(commandBuffer);
      submit(commandBuffer, imageIndex, currentFrame);
      resize = swapChain.presentImage(presentationQueue, renderingCompleteSemaphores.get(imageIndex), imageIndex);
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
      surface = VulkanSurface.createInstance(device.getPhysicalDevice(), window.getHandle());
      swapChain = VulkanSwapChain.createInstance(window, BUFFERING_SETUP, VSYNC);

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
      List<Attachment> attachments = new ArrayList<>(sceneRenderer.getMaterialAttachments().getColorAttachments());
      attachments.add(shadowRenderer.getColorAttachment());
      lightingRenderer.resize(device, memoryAllocationUtil, swapChain, descriptorAllocator, attachments);
      postProcessingRenderer.resize(device, memoryAllocationUtil, swapChain, descriptorAllocator, lightingRenderer.getAttachmentColor());
      guiRender.resize(device, swapChain, postProcessingRenderer.getColorAttachment());
      swapChainRender.resize(device, swapChain, descriptorAllocator, postProcessingRenderer.getColorAttachment());
   }

   private void submit(CommandBuffer buf, int imageIndex, int currentFrame) {
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

   private void waitForFence(int currentFrame) {
      var fence = fences.get(currentFrame);
      fence.fenceWait(device);
   }

   public void updateGlobalBuffers(VulkanScene scene, int currentFrame) {
      globalBuffers.update(device, memoryAllocationUtil, scene, currentFrame);
   }

   public LogicalDevice getDevice() {
      return device;
   }
}
