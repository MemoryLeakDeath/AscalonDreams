package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;
import java.util.List;

public class DescriptorSetLayout {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorSetLayout.class);

   private final List<DescriptorSetLayoutInfo> layoutInfos;
   private long id;

   public DescriptorSetLayout(LogicalDevice device, DescriptorSetLayoutInfo info) {
      this(device, List.of(info));
   }

   public DescriptorSetLayout(LogicalDevice device, List<DescriptorSetLayoutInfo> infos) {
      this.layoutInfos = infos;
      try(var stack = MemoryStack.stackPush()) {
         int count = layoutInfos.size();
         var bindings = VkDescriptorSetLayoutBinding.calloc(count, stack);
         for(int i = 0; i < count; i++) {
            var info = infos.get(i);
            bindings.get(i)
                    .binding(info.binding())
                    .descriptorType(info.descriptorType())
                    .descriptorCount(info.descriptorCount())
                    .stageFlags(info.stage());
         }
         var layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                 .sType$Default()
                 .pBindings(bindings);
         LongBuffer setLayoutBuf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateDescriptorSetLayout(device.getDevice(), layoutInfo, null, setLayoutBuf), "Failed to create descriptor set layout!");
         this.id = setLayoutBuf.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up descriptor set layout");
      VK13.vkDestroyDescriptorSetLayout(device.getDevice(), id, null);
   }

   public int[] getLayoutInfoTypes() {
      return layoutInfos.stream().mapToInt(DescriptorSetLayoutInfo::descriptorType).toArray();
   }

   public DescriptorSetLayoutInfo getLayoutInfo() {
      return layoutInfos.getFirst();
   }

   public List<DescriptorSetLayoutInfo> getLayoutInfos() {
      return layoutInfos;
   }

   public long getId() {
      return id;
   }
}
