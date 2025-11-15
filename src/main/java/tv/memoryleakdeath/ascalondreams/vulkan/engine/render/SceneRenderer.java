package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.util.ArrayList;
import java.util.List;

public class SceneRenderer {
   private static final Logger logger = LoggerFactory.getLogger(SceneRenderer.class);
   private VkClearValue clearValue;
   private List<VkRenderingAttachmentInfo.Buffer> colorAttachmentInfo = new ArrayList<>();
   private List<VkRenderingInfo> renderingInfos = new ArrayList<>();

   public SceneRenderer(VulkanSwapChain swapChain) {
      this.clearValue = VkClearValue.calloc().color(
              c -> c.float32(0, 0f)
                      .float32(1, 0f)
                      .float32(2, 0f)
                      .float32(3, 1f));
      initColorAttachmentsInfo(swapChain);
      initRenderInfos(swapChain);
   }

   private void initColorAttachmentsInfo(VulkanSwapChain swapChain) {
      int numImages = swapChain.getNumImages();
      for(int i = 0; i < numImages; i++) {
         colorAttachmentInfo.add(VkRenderingAttachmentInfo.calloc(1)
                 .sType$Default()
                 .imageView(swapChain.getImageViews().get(i).getImageViewId())
                 .imageLayout(KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                 .clearValue(clearValue));
      }
   }

   private void initRenderInfos(VulkanSwapChain swapChain) {
      assert !colorAttachmentInfo.isEmpty() : "Cannot init render infos!  Are these in the correct order?";
      try(var stack = MemoryStack.stackPush()) {
         var renderArea = VkRect2D.calloc(stack).extent(swapChain.getSwapChainExtent());
         renderingInfos = colorAttachmentInfo.stream().map(i -> VkRenderingInfo.calloc()
                 .sType$Default()
                 .renderArea(renderArea)
                 .layerCount(1)
                 .pColorAttachments(i)).toList();
      }
   }

   public void cleanup() {
      renderingInfos.forEach(VkRenderingInfo::free);
      colorAttachmentInfo.forEach(VkRenderingAttachmentInfo.Buffer::free);
      clearValue.free();
   }

   public void render(VulkanSwapChain swapChain, CommandBuffer commandBuffer, int imageIndex) {
      try(var stack = MemoryStack.stackPush()) {
         long swapChainImage = swapChain.getImageView(imageIndex).getImageId();
         var commandHandle = commandBuffer.getCommandBuffer();
         StructureUtils.imageBarrier(stack, commandHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         VK13.vkCmdBeginRendering(commandHandle, renderingInfos.get(imageIndex));
         VK13.vkCmdEndRendering(commandHandle);
         StructureUtils.imageBarrier(stack, commandHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT,
                 VK13.VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_PIPELINE_STAGE_2_NONE, VK13.VK_IMAGE_ASPECT_COLOR_BIT);
      }
   }
}
