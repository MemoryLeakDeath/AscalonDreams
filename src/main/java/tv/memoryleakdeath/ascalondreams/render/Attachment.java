package tv.memoryleakdeath.ascalondreams.render;

import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;

public class Attachment {
   private static final Logger logger = LoggerFactory.getLogger(Attachment.class);

   private final VulkanImage image;
   private final VulkanImageView imageView;
   private boolean depthAttachment = false;

   public Attachment(LogicalDevice device, MemoryAllocationUtil allocationUtil, int width, int height, int format, int usage, int layers) {
      int imageUsage = -1;
      int aspectMask = 0;
      if((usage & VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0) {
         aspectMask = VK13.VK_IMAGE_ASPECT_COLOR_BIT;
         imageUsage = (usage | VK13.VK_IMAGE_USAGE_SAMPLED_BIT | VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
         depthAttachment = false;
      }
      if((usage & VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) {
         aspectMask = VK13.VK_IMAGE_ASPECT_DEPTH_BIT;
         imageUsage = (usage | VK13.VK_IMAGE_USAGE_SAMPLED_BIT);
         depthAttachment = true;
      }
      this.image = new VulkanImage(allocationUtil, width, height, imageUsage, format, 1,
              Vma.VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT, layers);

      var imageViewData = new VulkanImageViewData();
      imageViewData.setFormat(image.getFormat());
      imageViewData.setAspectMask(aspectMask);
      if(layers > 1) {
         imageViewData.setViewType(VK13.VK_IMAGE_VIEW_TYPE_2D_ARRAY);
         imageViewData.setLayerCount(layers);
      }
      this.imageView = new VulkanImageView(device, image.getId(), imageViewData, depthAttachment);
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      imageView.cleanup();
      image.cleanup(device, allocationUtil);
   }

   public VulkanImage getImage() {
      return image;
   }

   public VulkanImageView getImageView() {
      return imageView;
   }

   public boolean isDepthAttachment() {
      return depthAttachment;
   }
}
