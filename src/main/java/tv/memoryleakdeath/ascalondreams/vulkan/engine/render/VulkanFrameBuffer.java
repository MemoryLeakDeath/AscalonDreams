package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanFrameBuffer {
   private LogicalDevice device;
   private long id;

   public VulkanFrameBuffer(LogicalDevice device, int width, int height, LongBuffer attachments, long renderPassId) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                 .pAttachments(attachments)
                 .width(width)
                 .height(height)
                 .layers(1)
                 .renderPass(renderPassId);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateFramebuffer(device.getDevice(), framebufferCreateInfo, null, buf), "Cannot create framebuffer!");
         this.id = buf.get(0);
      }
   }

   public void cleanup() {
      VK14.vkDestroyFramebuffer(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
