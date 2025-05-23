package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class Semaphore {
   private final LogicalDevice device;
   private final long id;

   public Semaphore(LogicalDevice device) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createSemaphoreInfo(stack, device.getDevice());
      }
   }

   public void cleanup() {
      VK14.vkDestroySemaphore(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
