package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors;

import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.TextureSampler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.util.List;

public class DescriptorSet {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorSet.class);
   private final long id;

   public DescriptorSet(LogicalDevice device, DescriptorPool pool, DescriptorSetLayout layout) {
      try(MemoryStack stack = MemoryStack.stackPush()) {
         this.id = StructureUtils.createDescriptorSet(stack, device, pool.getId(), layout.getId());
      }
   }

   public void setBuffer(LogicalDevice device, VulkanBuffer buffer, long range, int binding, int type) {
      try(MemoryStack stack = MemoryStack.stackPush()) {
         StructureUtils.updateDescriptorBuffer(stack, device, buffer, id, range, binding, type);
      }
   }

   public void setImages(LogicalDevice device, List<VulkanImageView> imageViews, TextureSampler sampler, int baseBinding) {
      try(MemoryStack stack = MemoryStack.stackPush()) {
         StructureUtils.updateDescriptorImageBuffer(stack, device, id, imageViews, sampler.getId(), baseBinding);
      }
   }

   public void setImagesArray(LogicalDevice device, List<VulkanImageView> imageViews, TextureSampler sampler, int baseBinding) {
      try(MemoryStack stack = MemoryStack.stackPush()) {
         StructureUtils.updateDescriptorImageBufferArray(stack, device, id, imageViews, sampler.getId(), baseBinding);
      }
   }

   public long getId() {
      return id;
   }
}
