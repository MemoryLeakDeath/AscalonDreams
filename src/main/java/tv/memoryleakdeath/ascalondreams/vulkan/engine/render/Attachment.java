package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;

public class Attachment {
   private static final Logger logger = LoggerFactory.getLogger(Attachment.class);

   private final VulkanImage image;
   private final VulkanImageView imageView;
   private boolean depthAttachment = false;

   public Attachment(LogicalDevice device, int width, int height, int format, int usage) {
      int imageUsage = (usage | VK13.VK_IMAGE_USAGE_SAMPLED_BIT);
      this.image = new VulkanImage(device, width, height, imageUsage, format);

      int aspectMask = 0;
      if((usage & VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0) {
         aspectMask = VK13.VK_IMAGE_ASPECT_COLOR_BIT;
         depthAttachment = false;
      }
      if((usage & VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) {
         aspectMask = VK13.VK_IMAGE_ASPECT_DEPTH_BIT;
         depthAttachment = true;
      }
      var imageViewData = new VulkanImageViewData();
      imageViewData.setFormat(image.getFormat());
      imageViewData.setAspectMask(aspectMask);
      this.imageView = new VulkanImageView(device, image.getId(), imageViewData, depthAttachment);
   }

   public void cleanup(LogicalDevice device) {
      imageView.cleanup();
      image.cleanup(device);
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
