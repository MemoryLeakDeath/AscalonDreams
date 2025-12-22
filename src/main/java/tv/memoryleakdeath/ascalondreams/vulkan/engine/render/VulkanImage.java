package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanImage {
   private static final Logger logger = LoggerFactory.getLogger(VulkanImage.class);

   private final int format;
   private final int mipLevels;
   private final long id;
   private final long memoryHandle;

   public VulkanImage(LogicalDevice device, int width, int height, int usage, int imageFormat, int mipLevels) {
      try(var stack = MemoryStack.stackPush()) {
         this.format = imageFormat;
         this.mipLevels = mipLevels;
         var info = VkImageCreateInfo.calloc(stack)
                 .sType$Default()
                 .imageType(VK13.VK_IMAGE_TYPE_2D)
                 .format(format)
                 .extent(ex -> ex.width(width).height(height).depth(1))
                 .mipLevels(mipLevels)
                 .arrayLayers(1)
                 .samples(1)
                 .initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED)
                 .sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE)
                 .tiling(VK13.VK_IMAGE_TILING_OPTIMAL)
                 .usage(usage);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateImage(device.getDevice(), info, null, buf), "Failed to create image!");
         this.id = buf.get(0);

         // get memory requirements
         VkMemoryRequirements requirements = VkMemoryRequirements.calloc(stack);
         VK13.vkGetImageMemoryRequirements(device.getDevice(), id, requirements);

         // memory size and type
         var allocInfo = VkMemoryAllocateInfo.calloc(stack)
                 .sType$Default()
                 .allocationSize(requirements.size())
                 .memoryTypeIndex(VulkanUtils.getMemoryType(device, requirements.memoryTypeBits(), 0));

         // allocate
         VulkanUtils.failIfNeeded(VK13.vkAllocateMemory(device.getDevice(), allocInfo, null, buf), "Failed to allocate memory!");
         this.memoryHandle = buf.get(0);

         // bind
         VulkanUtils.failIfNeeded(VK13.vkBindImageMemory(device.getDevice(), id, memoryHandle, 0), "Failed to bind image memory!");
      }
   }

   public void cleanup(LogicalDevice device) {
      VK13.vkDestroyImage(device.getDevice(), id, null);
      VK13.vkFreeMemory(device.getDevice(), memoryHandle, null);
   }

   public int getFormat() {
      return format;
   }

   public int getMipLevels() {
      return mipLevels;
   }

   public long getId() {
      return id;
   }
}
