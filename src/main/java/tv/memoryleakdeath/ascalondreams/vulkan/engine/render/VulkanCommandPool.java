package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanCommandPool {
   private final LogicalDevice device;
   private final long id;

   public VulkanCommandPool(LogicalDevice device, int queueFamilyIndex) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                 .flags(VK14.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                 .queueFamilyIndex(queueFamilyIndex);

         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateCommandPool(device.getDevice(), poolInfo, null, buf), "Cannot create command pool!");
         this.id = buf.get(0);
      }
   }

   public void cleanup() {
      VK14.vkDestroyCommandPool(device.getDevice(), id, null);
   }
   
   public LogicalDevice getDevice() {
      return device;
   }

   public long getId() {
      return id;
   }
}
