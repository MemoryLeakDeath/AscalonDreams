package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.nio.LongBuffer;

public class VulkanFrameBuffer {
   private static final Logger logger = LoggerFactory.getLogger(VulkanFrameBuffer.class);
   private LogicalDevice device;
   private long id;

   public VulkanFrameBuffer(LogicalDevice device, int width, int height, LongBuffer attachments, long renderPassId) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createFramebufferInfo(stack, device.getDevice(), attachments, width, height, 1, renderPassId);
         logger.debug("Created Frame Buffer with id: {}", id);
      }
   }

   public void cleanup() {
      logger.debug("Cleaning up framebuffer id: {}", id);
      VK14.vkDestroyFramebuffer(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }

   @Override
   public String toString() {
      return "VulkanFrameBuffer{" +
              "id=" + id +
              '}';
   }
}
