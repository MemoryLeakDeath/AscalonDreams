package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkViewport;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanFrameBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSwapChainRenderPass;

public final class ViewportUtils {
   private ViewportUtils() {
   }

   public static VkRenderPassBeginInfo initViewport(MemoryStack stack, VulkanSwapChainRenderPass renderPass, int swapChainWidth, int swapChainHeight, VulkanFrameBuffer frameBuffer) {
      VkClearValue.Buffer clearValue = VkClearValue.calloc(2, stack);
      clearValue.apply(0, v -> v.color().float32(0, 0).float32(1, 0).float32(2, 0).float32(3, 1));
      clearValue.apply(1, v -> v.depthStencil().depth(1.0f));

      VkRenderPassBeginInfo beginInfo = VkRenderPassBeginInfo.calloc(stack)
              .sType(VK14.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
              .renderPass(renderPass.getId())
              .pClearValues(clearValue)
              .renderArea(a -> a.extent().set(swapChainWidth, swapChainHeight))
              .framebuffer(frameBuffer.getId());
      return beginInfo;
   }

   public static void createViewport(MemoryStack stack, int swapChainHeight, int swapChainWidth, VkCommandBuffer commandBuffer) {
      VkViewport.Buffer viewportBuffer = VkViewport.calloc(1, stack)
              .x(0f)
              .y(swapChainHeight)
              .height(-swapChainHeight)
              .width(swapChainWidth)
              .minDepth(0f)
              .maxDepth(1f);
      VK14.vkCmdSetViewport(commandBuffer, 0, viewportBuffer);
   }

   public static void createScissor(MemoryStack stack, int swapChainWidth, int swapChainHeight, VkCommandBuffer commandBuffer) {
      VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
              .extent(it -> it.width(swapChainWidth).height(swapChainHeight))
              .offset(it -> it.x(0).y(0));
      VK14.vkCmdSetScissor(commandBuffer, 0, scissor);
   }
}
