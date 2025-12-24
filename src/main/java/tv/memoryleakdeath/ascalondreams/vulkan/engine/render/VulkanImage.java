package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

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

   public VulkanImage(LogicalDevice device, MemoryAllocationUtil allocationUtil, int width, int height, int usage, int imageFormat, int mipLevels, int memoryUsage) {
      try(var stack = MemoryStack.stackPush()) {
         this.format = imageFormat;
         this.mipLevels = mipLevels;
         this.memoryUsage = memoryUsage;
         this.width = width;
         this.height = height;
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
}
