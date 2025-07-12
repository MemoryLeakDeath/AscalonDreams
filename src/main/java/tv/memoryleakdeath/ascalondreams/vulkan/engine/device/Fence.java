package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class Fence {
   private static final Logger logger = LoggerFactory.getLogger(Fence.class);
   private final LogicalDevice device;
   private final long id;

   public Fence(LogicalDevice device, boolean signaled) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkFenceCreateInfo info = VkFenceCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                 .flags(signaled ? VK14.VK_FENCE_CREATE_SIGNALED_BIT : 0);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateFence(device.getDevice(), info, null, buf), "Could not create fence!");
         this.id = buf.get(0);
         logger.debug("Created fence with id: {}", id);
      }
   }

   public void cleanup() {
      logger.debug("cleaning up fence with id: {}", id);
      VK14.vkDestroyFence(device.getDevice(), id, null);
   }

   public void waitForFence() {
      logger.debug("Waiting for fence: {}", id);
      VK14.vkWaitForFences(device.getDevice(), id, true, Long.MAX_VALUE);
   }

   public void reset() {
      logger.debug("resetting fence: {}", id);
      VK14.vkResetFences(device.getDevice(), id);
   }

   public long getId() {
      return id;
   }
}
