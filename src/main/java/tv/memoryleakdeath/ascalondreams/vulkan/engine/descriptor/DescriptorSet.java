package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;
import java.util.List;

public class DescriptorSet {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorSet.class);

   private long id;

   public DescriptorSet(LogicalDevice device, DescriptorPool pool, DescriptorSetLayout layout) {
      try(var stack = MemoryStack.stackPush()) {
         LongBuffer layoutBuf = stack.mallocLong(1);
         layoutBuf.put(0, layout.getId());
         var allocationInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                 .sType$Default()
                 .descriptorPool(pool.getId())
                 .pSetLayouts(layoutBuf);
         LongBuffer setBuf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkAllocateDescriptorSets(device.getDevice(), allocationInfo, setBuf), "Failed to create descriptor set!");
         this.id = setBuf.get(0);
      }
   }

   public void setBuffer(LogicalDevice device, VulkanBuffer buffer, long range, int binding, int type) {
      try(var stack = MemoryStack.stackPush()) {
         var info = VkDescriptorBufferInfo.calloc(1, stack)
                 .buffer(buffer.getBuffer())
                 .offset(0)
                 .range(range);
         var descriptorBuffer = VkWriteDescriptorSet.calloc(1, stack)
                 .sType$Default()
                 .dstSet(id)
                 .dstBinding(binding)
                 .descriptorType(type)
                 .descriptorCount(1)
                 .pBufferInfo(info);
         VK13.vkUpdateDescriptorSets(device.getDevice(), descriptorBuffer, null);
      }
   }

   public void setImage(LogicalDevice device, VulkanImageView imageView, VulkanTextureSampler sampler, int binding) {
      setImages(device, List.of(imageView), sampler, binding);
   }

   public void setImages(LogicalDevice device, List<VulkanImageView> imageViews, VulkanTextureSampler sampler, int binding) {
      try (var stack = MemoryStack.stackPush()) {
         int numImages = imageViews.size();
         var buf = VkWriteDescriptorSet.calloc(numImages, stack);
         for(int i = 0; i < numImages; i++) {
            VulkanImageView imageView = imageViews.get(i);
            var info = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(imageView.getImageViewId())
                    .sampler(sampler.getId());
            if(imageView.isDepthImage()) {
               info.imageLayout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
            } else {
               info.imageLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }

            buf.get(i)
                    .sType$Default()
                    .dstSet(id)
                    .dstBinding(binding + i)
                    .descriptorType(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(info);
         }
         VK13.vkUpdateDescriptorSets(device.getDevice(), buf, null);
      }
   }

   public void setImagesArray(LogicalDevice device, List<VulkanImageView> imageViews, VulkanTextureSampler sampler, int binding) {
      try(var stack = MemoryStack.stackPush()) {
         int numImages = imageViews.size();
         var infos = VkDescriptorImageInfo.calloc(numImages, stack);
         for(int i = 0; i < numImages; i++) {
            VulkanImageView view = imageViews.get(i);
            var info = infos.get(i);
            info.imageView(view.getImageViewId())
                    .sampler(sampler.getId());
            if(view.isDepthImage()) {
               info.imageLayout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
            } else {
               info.imageLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }
         }

         var buf = VkWriteDescriptorSet.calloc(1, stack)
                 .sType$Default()
                 .dstSet(id)
                 .dstBinding(binding)
                 .dstArrayElement(0)
                 .descriptorType(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                 .descriptorCount(numImages)
                 .pImageInfo(infos);
         VK13.vkUpdateDescriptorSets(device.getDevice(), buf, null);
      }
   }

   public long getId() {
      return id;
   }
}
