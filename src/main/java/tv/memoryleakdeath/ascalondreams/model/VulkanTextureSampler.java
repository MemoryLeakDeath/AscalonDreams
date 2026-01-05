package tv.memoryleakdeath.ascalondreams.model;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanTextureSampler {
   private static final Logger logger = LoggerFactory.getLogger(VulkanTextureSampler.class);
   private static final int MAX_ANISOTROPY = 16;
   private final long id;

   public VulkanTextureSampler(LogicalDevice device, int addressMode, int borderColor, int miplevels, boolean anisotropy) {
      try(var stack = MemoryStack.stackPush()) {
         var info = VkSamplerCreateInfo.calloc(stack)
                 .sType$Default()
                 .magFilter(VK13.VK_FILTER_NEAREST)
                 .minFilter(VK13.VK_FILTER_NEAREST)
                 .addressModeU(addressMode)
                 .addressModeV(addressMode)
                 .addressModeW(addressMode)
                 .borderColor(borderColor)
                 .unnormalizedCoordinates(false)
                 .compareEnable(false)
                 .compareOp(VK13.VK_COMPARE_OP_NEVER)
                 .mipmapMode(VK13.VK_SAMPLER_MIPMAP_MODE_NEAREST)
                 .minLod(0f)
                 .maxLod(miplevels)
                 .mipLodBias(0f);
         if(anisotropy && device.isSamplerAnisotropy()) {
            info.anisotropyEnable(true).maxAnisotropy(MAX_ANISOTROPY);
         }

         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateSampler(device.getDevice(), info, null, buf), "Failed to create sampler!");
         this.id = buf.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      VK13.vkDestroySampler(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }
}
