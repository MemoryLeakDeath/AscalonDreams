package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Semaphore;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;

import java.util.ArrayList;
import java.util.List;

public class VulkanRenderer {
   private final VulkanRenderInstance instance;
   private static final boolean VSYNC = true;
   private static final int BUFFERING_SETUP = 3;
   private static final int MAX_IN_FLIGHT = 2;
   private VulkanWindow window;
   private final LogicalDevice device;
   private final VulkanSurface surface;
   private final VulkanSwapChain swapChain;

   private final List<CommandBuffer> commandBuffers = new ArrayList<>();
   private final List<CommandPool> commandPools = new ArrayList<>();
   private final List<Fence> fences = new ArrayList<>();
   private final VulkanGraphicsQueue graphicsQueue;
   private final List<Semaphore> presentationCompleteSemaphores = new ArrayList<>();
   private final VulkanPresentationQueue presentationQueue;
   private final List<Semaphore> renderingCompleteSemaphores = new ArrayList<>();
   private final SceneRenderer sceneRenderer;
   private int currentFrame = 0;



   public VulkanRenderer(VulkanWindow window) {
      this.window = window;
      this.instance = new VulkanRenderInstance(false);
      this.device = new LogicalDevice(PhysicalDevice.getInstance(instance.getVkInstance()));
      this.surface = new VulkanSurface(device.getPhysicalDevice(), window.getHandle());
      this.swapChain = new VulkanSwapChain(device, surface, window, BUFFERING_SETUP, VSYNC);

      this.graphicsQueue = new VulkanGraphicsQueue(device, 0);
      this.presentationQueue = new VulkanPresentationQueue(device, surface, 0);

      for(int i = 0; i < MAX_IN_FLIGHT; i++) {
         CommandPool pool = new CommandPool(device, graphicsQueue.getQueueFamilyIndex(), false);
         commandPools.add(pool);
         commandBuffers.add(new CommandBuffer(device, pool, true, true));
         presentationCompleteSemaphores.add(new Semaphore(device));
         fences.add(new Fence(device, true));
      }

      for(int i = 0; i < swapChain.getNumImages(); i++) {
         renderingCompleteSemaphores.add(new Semaphore(device));
      }

      this.sceneRenderer = new SceneRenderer(swapChain);
   }

   public void cleanup() {
      device.waitIdle();

      sceneRenderer.cleanup();

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

   private void startRecording(CommandPool pool, CommandBuffer buf) {
      pool.reset(device);
      buf.beginRecording();
   }

   private void stopRecording(CommandBuffer buf) {
      buf.endRecording();
   }

   public void render() {
      waitForFence();
      var commandPool = commandPools.get(currentFrame);
      var commandBuffer = commandBuffers.get(currentFrame);

      startRecording(commandPool, commandBuffer);

      int imageIndex = swapChain.acquireNextImage(device, presentationCompleteSemaphores.get(currentFrame));
      if(imageIndex < 0) {
         return;
      }
      sceneRenderer.render(swapChain, commandBuffer, imageIndex);

      stopRecording(commandBuffer);
      submit(commandBuffer, imageIndex);
      swapChain.presentImage(presentationQueue, renderingCompleteSemaphores.get(imageIndex), imageIndex);

      currentFrame = (currentFrame + 1) % MAX_IN_FLIGHT;
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
}
