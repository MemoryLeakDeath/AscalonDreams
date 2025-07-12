package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class Semaphore {
   private static final Logger logger = LoggerFactory.getLogger(Semaphore.class);
   private final LogicalDevice device;
   private final long id;

   public Semaphore(LogicalDevice device) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createSemaphoreInfo(stack, device.getDevice());
         logger.debug("Created Semaphore: {}", id);
      }
   }

   public void cleanup() {
      logger.debug("Cleanup semaphore: {}", id);
      VK14.vkDestroySemaphore(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
