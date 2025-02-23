package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkQueue;

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
}
