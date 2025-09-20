package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;
import java.util.List;

public class DescriptorPool {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorPool.class);
   private final long id;
   private List<DescriptorTypeCount> typeCounts;

   public DescriptorPool(LogicalDevice device, List<DescriptorTypeCount> typeCounts) {
      logger.debug("Initializing descriptor pool.....");
      this.typeCounts = typeCounts;
      try(MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createDescriptorPoolInfo(stack, device, 0, typeCounts);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up descriptor pool");
      VK14.vkDestroyDescriptorPool(device.getDevice(), id, null);
   }

   public void freeDescriptorSet(LogicalDevice device, long descriptorSetId) {
      try(MemoryStack stack = MemoryStack.stackPush()) {
         LongBuffer descriptorSetBuffer = stack.mallocLong(1);
         descriptorSetBuffer.put(0, descriptorSetId);
         VulkanUtils.failIfNeeded(VK14.vkFreeDescriptorSets(device.getDevice(), id, descriptorSetBuffer), "Failed to free descriptor set with id: %d".formatted(descriptorSetId));
      }
   }

   public long getId() {
      return id;
   }

   public List<DescriptorTypeCount> getTypeCounts() {
      return typeCounts;
   }
}
