package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.util.List;

public class DescriptorSetLayout {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorSetLayout.class);
   private List<LayoutInfo> layoutInfoList;
   private final long id;

   public DescriptorSetLayout(LogicalDevice device, List<LayoutInfo> layoutInfoList) {
      this.layoutInfoList = layoutInfoList;
      try(MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createDescriptorSetLayout(stack, device, layoutInfoList);
      }
   }

   public void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up descriptor set layout...");
      VK14.vkDestroyDescriptorSetLayout(device.getDevice(), id, null);
   }

   public List<LayoutInfo> getLayoutInfoList() {
      return layoutInfoList;
   }

   public List<Integer> getLayoutInfoDescriptorTypes() {
      return layoutInfoList.stream().map(LayoutInfo::type).toList();
   }

   public LayoutInfo getLayoutInfo() {
      return layoutInfoList.stream().findFirst().orElse(null);
   }

   public long getId() {
      return id;
   }
}
