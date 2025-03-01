package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public abstract class BaseDeviceQueue {
   private VkQueue queue;
   private int queueFamilyIndex;

   protected void createQueue(LogicalDevice device, int queueFamilyIndex, int queueIndex) {
      this.queueFamilyIndex = queueFamilyIndex;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         PointerBuffer queuePointer = stack.mallocPointer(1);
         VK14.vkGetDeviceQueue(device.getDevice(), queueFamilyIndex, queueIndex, queuePointer);
         this.queue = new VkQueue(queuePointer.get(0), device.getDevice());
      }
   }

   public int getQueueFamilyIndex() {
      return queueFamilyIndex;
   }

   public void waitIdle() {
      VK14.vkQueueWaitIdle(queue);
   }

   public VkQueue getQueue() {
      return queue;
   }

   public void submit(PointerBuffer commandBuffers, LongBuffer waitSemaphores, IntBuffer destinationStageMasks, LongBuffer signalSemaphores, Fence fence) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                 .pCommandBuffers(commandBuffers)
                 .pSignalSemaphores(signalSemaphores);
         if (waitSemaphores != null) {
            submitInfo.waitSemaphoreCount(waitSemaphores.capacity())
                    .pWaitSemaphores(waitSemaphores)
                    .pWaitDstStageMask(destinationStageMasks);
         } else {
            submitInfo.waitSemaphoreCount(0);
         }
         long fenceHandle = fence != null ? fence.getId() : VK14.VK_NULL_HANDLE;
         VulkanUtils.failIfNeeded(VK14.vkQueueSubmit(queue, submitInfo, fenceHandle), "Failed to submit command to queue!");
      }
   }
}
