package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;

public class VulkanAttachment {
   private VulkanImage image;
   private VulkanImageView view;
   private boolean depthAttachment = false;

   public VulkanAttachment(LogicalDevice device, int width, int height, int format, int usage) {
      VulkanImageData imageData = new VulkanImageData();
      imageData.setWidth(width);
      imageData.setHeight(height);
      imageData.setFormat(format);
      imageData.setUsage(usage | VK14.VK_IMAGE_USAGE_SAMPLED_BIT);

      this.image = new VulkanImage(device, imageData);

      int aspectMask = 0;
      if ((usage & VK14.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0) {
         aspectMask = VK14.VK_IMAGE_ASPECT_COLOR_BIT;
      }
      if ((usage & VK14.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) {
         aspectMask = VK14.VK_IMAGE_ASPECT_DEPTH_BIT;
         this.depthAttachment = true;
      }

      VulkanImageViewData imageViewData = new VulkanImageViewData();
      imageViewData.setFormat(image.getImageData().getFormat());
      imageViewData.setAspectMask(aspectMask);
      this.view = new VulkanImageView(device, image.getImageId(), imageViewData);
   }

   public void cleanup() {
      view.cleanup();
      image.cleanup();
   }

   public VulkanImage getImage() {
      return image;
   }

   public VulkanImageView getView() {
      return view;
   }

   public boolean isDepthAttachment() {
      return depthAttachment;
   }
}
