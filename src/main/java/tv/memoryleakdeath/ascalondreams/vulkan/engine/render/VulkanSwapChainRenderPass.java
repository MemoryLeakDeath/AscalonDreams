package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class VulkanSwapChainRenderPass {
   private VulkanSwapChain swapChain;
   private long id;

   public VulkanSwapChainRenderPass(VulkanSwapChain swapChain, int depthImageFormat) {
      this.swapChain = swapChain;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, stack);

         // color attachment
         attachments.get(0).format(swapChain.getSurfaceFormats().imageFormat())
                 .samples(VK14.VK_SAMPLE_COUNT_1_BIT)
                 .loadOp(VK14.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK14.VK_ATTACHMENT_STORE_OP_STORE)
                 .initialLayout(VK14.VK_IMAGE_LAYOUT_UNDEFINED)
                 .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

         // depth attachment
         attachments.get(1).format(depthImageFormat)
                 .samples(VK14.VK_SAMPLE_COUNT_1_BIT)
                 .loadOp(VK14.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK14.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                 .initialLayout(VK14.VK_IMAGE_LAYOUT_UNDEFINED)
                 .finalLayout(VK14.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

         VkAttachmentReference.Buffer colorReference = VkAttachmentReference.calloc(1, stack)
                 .attachment(0)
                 .layout(VK14.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

         VkAttachmentReference depthReference = VkAttachmentReference.malloc(stack)
                 .attachment(1)
                 .layout(VK14.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

         VkSubpassDescription.Buffer subpassDescription = VkSubpassDescription.calloc(1, stack)
                 .pipelineBindPoint(VK14.VK_PIPELINE_BIND_POINT_GRAPHICS)
                 .colorAttachmentCount(colorReference.capacity())
                 .pColorAttachments(colorReference)
                 .pDepthStencilAttachment(depthReference);

         VkSubpassDependency.Buffer subpassDependencies = VkSubpassDependency.calloc(1, stack);
         subpassDependencies.get(0).srcSubpass(VK14.VK_SUBPASS_EXTERNAL)
                 .dstSubpass(0)
                 .srcStageMask(VK14.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK14.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                 .dstStageMask(VK14.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK14.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                 .srcAccessMask(0)
                 .dstAccessMask(VK14.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK14.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

         this.id = StructureUtils.createRenderPass(stack, attachments, subpassDescription, subpassDependencies, swapChain.getDevice().getDevice());
      }
   }

   public void cleanup() {
      VK14.vkDestroyRenderPass(swapChain.getDevice().getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
