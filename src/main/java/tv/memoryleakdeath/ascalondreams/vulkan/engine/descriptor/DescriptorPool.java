package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;
import java.util.List;

public class DescriptorPool {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorPool.class);

   private final long id;
   private List<DescriptorTypeCount> descriptorTypeCounts;

   public DescriptorPool(LogicalDevice device, List<DescriptorTypeCount> typeCounts) {
      logger.debug("Creating descriptor pool....");
      this.descriptorTypeCounts = typeCounts;
      try(var stack = MemoryStack.stackPush()) {
         int maxSets = 0;
         int numTypes = descriptorTypeCounts.size();
         var counts = VkDescriptorPoolSize.calloc(numTypes, stack);
         for(int i = 0; i < numTypes; i++) {
            maxSets += typeCounts.get(i).count();
            counts.get(i)
                    .type(typeCounts.get(i).descriptorType())
                    .descriptorCount(typeCounts.get(i).count());
         }

         var poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                 .sType$Default()
                 .flags(VK13.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                 .pPoolSizes(counts)
                 .maxSets(maxSets);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateDescriptorPool(device.getDevice(), poolInfo, null, buf), "Failed to create descriptor pool!");
         this.id = buf.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up descriptor pool");
      VK13.vkDestroyDescriptorPool(device.getDevice(), id, null);
   }

   public void freeDescriptorSet(LogicalDevice device, long descriptorSetId) {
      try(var stack = MemoryStack.stackPush()) {
         LongBuffer buf = stack.mallocLong(1);
         buf.put(0, descriptorSetId);
         VulkanUtils.failIfNeeded(VK13.vkFreeDescriptorSets(device.getDevice(), id, buf), "Failed to free descriptor set!");
      }
   }

   public long getId() {
      return id;
   }

   public List<DescriptorTypeCount> getDescriptorTypeCounts() {
      return descriptorTypeCounts;
   }
}
