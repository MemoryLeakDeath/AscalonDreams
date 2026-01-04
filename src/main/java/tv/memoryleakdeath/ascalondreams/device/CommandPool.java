package tv.memoryleakdeath.ascalondreams.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.util.StructureUtils;

public class CommandPool {
   private static final Logger logger = LoggerFactory.getLogger(CommandPool.class);
   private long id;

   public CommandPool(LogicalDevice device, int queueFamilyIndex, boolean supportReset) {
      logger.debug("Creating command pool!");
      try(var stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createCommandPool(stack, device, queueFamilyIndex, supportReset);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up command pool");
      VK13.vkDestroyCommandPool(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }

   public void reset(LogicalDevice device) {
      VK13.vkResetCommandPool(device.getDevice(), id, 0);
   }
}
