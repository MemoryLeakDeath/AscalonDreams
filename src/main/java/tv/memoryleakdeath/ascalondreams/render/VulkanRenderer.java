package tv.memoryleakdeath.ascalondreams.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.cache.AnimationCache;
import tv.memoryleakdeath.ascalondreams.buffers.GlobalBuffers;
import tv.memoryleakdeath.ascalondreams.cache.MaterialCache;
import tv.memoryleakdeath.ascalondreams.cache.ModelCache;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.cache.TextureCache;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.device.Fence;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.device.Semaphore;
import tv.memoryleakdeath.ascalondreams.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.render.gui.GuiTexture;
import tv.memoryleakdeath.ascalondreams.model.VulkanMaterial;
import tv.memoryleakdeath.ascalondreams.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.model.conversion.ConvertedModel;
import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;
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

   private final AnimationCache animationCache = AnimationCache.getInstance();
   private final List<CommandBuffer> commandBuffers = new ArrayList<>();
   private final List<CommandPool> commandPools = new ArrayList<>();
   private final List<Fence> fences = new ArrayList<>();
   private final GlobalBuffers globalBuffers = GlobalBuffers.getInstance();
   private final VulkanGraphicsQueue graphicsQueue;
   private final List<Semaphore> presentationCompleteSemaphores = new ArrayList<>();
   private final VulkanPresentationQueue presentationQueue;
   private final List<Semaphore> renderingCompleteSemaphores = new ArrayList<>();
   private boolean resize = false;
   private final MemoryAllocationUtil memoryAllocationUtil;

   // singletons
   private final DescriptorAllocator descriptorAllocator;
   private final PipelineCache pipelineCache;
   private final ModelCache modelCache = ModelCache.getInstance();
   private final MaterialCache materialCache = MaterialCache.getInstance();
   private final TextureCache textureCache = TextureCache.getInstance();


   public VulkanRenderer(VulkanWindow window) {
      this.window = window;
      this.instance = new VulkanRenderInstance(true);
      this.device = DeviceManager.createDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = VulkanSurface.createInstance(device.getPhysicalDevice(), window.getHandle());
      this.swapChain = VulkanSwapChain.createInstance(window, BUFFERING_SETUP, VSYNC);
      this.memoryAllocationUtil = MemoryAllocationUtil.createInstance(instance);

      // these require device having been created already
      this.descriptorAllocator = DescriptorAllocator.getInstance();
      this.pipelineCache = PipelineCache.getInstance();

      this.graphicsQueue = VulkanGraphicsQueue.getInstance();
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
   }

   public void cleanup() {
      device.waitIdle();

      globalBuffers.cleanup(device, memoryAllocationUtil);
      RenderChain.cleanup();
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

   public void initModels(List<ConvertedModel> convertedModels, List<GuiTexture> guiTextures, List<String> skyboxTextures) {
      if(guiTextures != null) {
         guiTextures.forEach(t -> textureCache.addGuiTexture(t, VK13.VK_FORMAT_R8G8B8A8_SRGB));
      }
      if(skyboxTextures != null) {
         // TODO: id needs to be unique, fix this!  (look at GuiTexture record)
         skyboxTextures.forEach(t -> textureCache.addSkyboxTexture(device, memoryAllocationUtil, "SKYBOX_TEXTURE", t, VK13.VK_FORMAT_R8G8B8A8_UNORM));
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

      RenderChain.loadChain();
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

      int imageIndex;
      if(resize || (imageIndex = swapChain.acquireNextImage(device, presentationCompleteSemaphores.get(currentFrame))) < 0) {
         resize(window.getWidth(), window.getHeight(), scene);
         return;
      }
      startRecording(commandPool, commandBuffer);
      RenderChain.renderChain(commandBuffer, currentFrame, imageIndex);
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
      RenderChain.resizeChain();
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

}
