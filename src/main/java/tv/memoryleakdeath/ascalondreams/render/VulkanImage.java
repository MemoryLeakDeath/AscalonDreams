package tv.memoryleakdeath.ascalondreams.render;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanImage {
   private static final Logger logger = LoggerFactory.getLogger(VulkanImage.class);

   private final int format;
   private final int mipLevels;
   private final long id;
   private final int memoryUsage;
   private final long vmaAllocator;
   private final int width;
   private final int height;
   private final int layerCount;

   public VulkanImage(MemoryAllocationUtil allocationUtil, int width, int height, int usage,
                      int imageFormat, int mipLevels, int memoryUsage, int layerCount) {
      this(allocationUtil, width, height, usage, imageFormat, mipLevels, memoryUsage, layerCount, 0);
   }

   public VulkanImage(MemoryAllocationUtil allocationUtil, int width, int height, int usage,
                      int imageFormat, int mipLevels, int memoryUsage, int layerCount, int imageFlags) {
      try(var stack = MemoryStack.stackPush()) {
         this.format = imageFormat;
         this.mipLevels = mipLevels;
         this.memoryUsage = memoryUsage;
         this.width = width;
         this.height = height;
         this.layerCount = layerCount;
         var info = VkImageCreateInfo.calloc(stack)
                 .sType$Default()
                 .imageType(VK13.VK_IMAGE_TYPE_2D)
                 .format(format)
                 .extent(ex -> ex.width(width).height(height).depth(1))
                 .mipLevels(mipLevels)
                 .arrayLayers(layerCount)
                 .samples(1)
                 .initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED)
                 .sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE)
                 .tiling(VK13.VK_IMAGE_TILING_OPTIMAL)
                 .usage(usage);
         if(imageFlags != 0) {
            info.flags(imageFlags);
         }
         var vmaAllocCreateInfo = VmaAllocationCreateInfo.calloc(1, stack)
                 .get(0)
                 .usage(Vma.VMA_MEMORY_USAGE_AUTO)
                 .flags(this.memoryUsage)
                 .priority(1.0f);
         PointerBuffer allocBuf = stack.callocPointer(1);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(Vma.vmaCreateImage(allocationUtil.getVmaAllocator(), info, vmaAllocCreateInfo, buf, allocBuf, null), "Failed to create image!");
         this.id = buf.get(0);
         this.vmaAllocator = allocBuf.get(0);
      }
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      Vma.vmaDestroyImage(allocationUtil.getVmaAllocator(), id, vmaAllocator);
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

   public int getWidth() {
      return width;
   }

   public int getHeight() {
      return height;
   }

   public int getLayerCount() {
      return layerCount;
   }
}
