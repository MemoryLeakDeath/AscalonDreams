package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class Semaphore {
   private static final Logger logger = LoggerFactory.getLogger(Semaphore.class);
   private long id;

   public Semaphore(LogicalDevice device) {
      try(var stack = MemoryStack.stackPush()) {
         var info = VkSemaphoreCreateInfo.calloc(stack).sType$Default();
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateSemaphore(device.getDevice(), info, null, buf), "Failed to create semaphore!");
         this.id = buf.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      VK13.vkDestroySemaphore(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
