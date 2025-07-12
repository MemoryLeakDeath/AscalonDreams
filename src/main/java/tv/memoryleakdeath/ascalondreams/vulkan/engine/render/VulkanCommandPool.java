package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class VulkanCommandPool {
   private static final Logger logger = LoggerFactory.getLogger(VulkanCommandPool.class);
   private final LogicalDevice device;
   private final long id;

   public VulkanCommandPool(LogicalDevice device, int queueFamilyIndex) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createPoolInfo(stack, device.getDevice(), queueFamilyIndex);
         logger.debug("created command pool with id: {}", id);
      }
   }

   public void cleanup() {
      logger.debug("cleaning command pool: {}", id);
      VK14.vkDestroyCommandPool(device.getDevice(), id, null);
   }
   
   public LogicalDevice getDevice() {
      return device;
   }

   public long getId() {
      return id;
   }
}
