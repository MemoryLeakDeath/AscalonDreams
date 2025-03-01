package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class ForwardRenderer {
   private final List<VulkanCommandBuffer> commandBuffers = new ArrayList<>();
   private final List<Fence> fences = new ArrayList<>();
   private final List<VulkanFrameBuffer> frameBuffers = new ArrayList<>();
   private final VulkanSwapChainRenderPass renderPass;
   private final VulkanSwapChain swapChain;

   public ForwardRenderer(VulkanSwapChain swapChain, VulkanCommandPool pool) {
      this.swapChain = swapChain;
      this.renderPass = new VulkanSwapChainRenderPass(swapChain);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         LogicalDevice device = swapChain.getDevice();
         int width = swapChain.getWidth();
         int height = swapChain.getHeight();
         LongBuffer attachments = stack.mallocLong(1);

         swapChain.getImageViews().forEach(view -> {
            attachments.put(0, view.getId());
            VulkanFrameBuffer frameBuffer = new VulkanFrameBuffer(device, width, height, attachments, renderPass.getId());
            VulkanCommandBuffer commandBuffer = new VulkanCommandBuffer(pool, true, false);
            Fence fence = new Fence(device, true);
            this.frameBuffers.add(frameBuffer);
            this.commandBuffers.add(commandBuffer);
            this.fences.add(fence);
            recordCommandBuffer(stack, commandBuffer, frameBuffer, width, height);
         });
      }
   }

   private void recordCommandBuffer(MemoryStack stack, VulkanCommandBuffer commandBuffer, VulkanFrameBuffer frameBuffer, int width, int height) {
      VkClearValue.Buffer clearValue = VkClearValue.calloc(1, stack);
      clearValue.apply(0, v -> v.color().float32(0, 0).float32(1, 0).float32(2, 0).float32(3, 1));
      VkRenderPassBeginInfo beginInfo = VkRenderPassBeginInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
              .renderPass(renderPass.getId())
              .pClearValues(clearValue)
              .renderArea(a -> a.extent().set(width, height))
              .framebuffer(frameBuffer.getId());

      commandBuffer.beginRecording();
      VK14.vkCmdBeginRenderPass(commandBuffer.getBuffer(), beginInfo, VK14.VK_SUBPASS_CONTENTS_INLINE);
      VK14.vkCmdEndRenderPass(commandBuffer.getBuffer());
      commandBuffer.endRecording();
   }

   public void submit(BaseDeviceQueue queue) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         int currentFrame = swapChain.getCurrentFrame();
         Fence currentFence = fences.get(currentFrame);
         currentFence.reset();
         VulkanCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
         VulkanSwapChain.SyncSemaphores syncSemaphores = swapChain.getSemaphoreList().get(currentFrame);
         queue.submit(stack.pointers(commandBuffer.getBuffer()),
                 stack.longs(syncSemaphores.imageAquisitionSemaphore().getId()),
                 stack.ints(VK14.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                 stack.longs(syncSemaphores.renderCompleteSemaphore().getId()),
                 currentFence);
      }
   }

   public void waitForFence() {
      fences.get(swapChain.getCurrentFrame()).waitForFence();
   }

   public void cleanup() {
      frameBuffers.forEach(VulkanFrameBuffer::cleanup);
      renderPass.cleanup();
      commandBuffers.forEach(VulkanCommandBuffer::cleanup);
      fences.forEach(Fence::cleanup);
   }
}
