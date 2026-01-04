package tv.memoryleakdeath.ascalondreams.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.lwjgl.vulkan.VkSubmitInfo2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

public abstract class BaseDeviceQueue {
   private static final Logger logger = LoggerFactory.getLogger(BaseDeviceQueue.class);
   private VkQueue queue;
   private int queueFamilyIndex;

   protected void createQueue(LogicalDevice device, int queueFamilyIndex, int queueIndex) {
      logger.debug("Creating queue...");
      this.queueFamilyIndex = queueFamilyIndex;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         PointerBuffer queuePointer = stack.mallocPointer(1);
         VK13.vkGetDeviceQueue(device.getDevice(), queueFamilyIndex, queueIndex, queuePointer);
         this.queue = new VkQueue(queuePointer.get(0), device.getDevice());
      }
   }

   public int getQueueFamilyIndex() {
      return queueFamilyIndex;
   }

   public void waitIdle() {
      VK13.vkQueueWaitIdle(queue);
   }

   public VkQueue getQueue() {
      return queue;
   }

   public void submit(VkCommandBufferSubmitInfo.Buffer commandBuffers, VkSemaphoreSubmitInfo.Buffer waitSemaphores, VkSemaphoreSubmitInfo.Buffer signalSemaphores, Fence fence) {
      try(var stack = MemoryStack.stackPush()) {
         var info = VkSubmitInfo2.calloc(1, stack)
                 .sType$Default()
                 .pCommandBufferInfos(commandBuffers)
                 .pSignalSemaphoreInfos(signalSemaphores);
         if(waitSemaphores != null) {
            info.pWaitSemaphoreInfos(waitSemaphores);
         }
         long fenceId = fence != null ? fence.getId() : VK13.VK_NULL_HANDLE;
         VulkanUtils.failIfNeeded(VK13.vkQueueSubmit2(queue, info, fenceId), "Failed to submit command to queue!");
      }
   }
}
