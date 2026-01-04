package tv.memoryleakdeath.ascalondreams.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.LongBuffer;

public class Fence {
   private static final Logger logger = LoggerFactory.getLogger(Fence.class);
   private long id;

   public Fence(LogicalDevice device, boolean signaled) {
      try(var stack = MemoryStack.stackPush()) {
         var fenceInfo = VkFenceCreateInfo.calloc(stack)
                 .sType$Default()
                 .flags(signaled ? VK13.VK_FENCE_CREATE_SIGNALED_BIT : 0);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateFence(device.getDevice(), fenceInfo, null, buf), "Failed to create fence!");
         this.id = buf.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      VK13.vkDestroyFence(device.getDevice(), id, null);
   }

   public void fenceWait(LogicalDevice device) {
      VK13.vkWaitForFences(device.getDevice(), id, true, Long.MAX_VALUE);
   }

   public long getId() {
      return id;
   }

   public void reset(LogicalDevice device) {
      VK13.vkResetFences(device.getDevice(), id);
   }
}
