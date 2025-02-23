package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class Semaphore {
   private final LogicalDevice device;
   private final long id;

   public Semaphore(LogicalDevice device) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkSemaphoreCreateInfo info = VkSemaphoreCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateSemaphore(device.getDevice(), info, null, buf), "Could not create semaphore!");
         this.id = buf.get(0);
      }
   }

   public void cleanup() {
      VK14.vkDestroySemaphore(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
