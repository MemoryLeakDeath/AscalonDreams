package tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo;

import org.lwjgl.vulkan.VK14;

public class VulkanImageViewData {
   private int aspectMask;
   private int baseArrayLayer = 0;
   private int format;
   private int layerCount = 1;
   private int mipLevels = 1;
   private int viewType = VK14.VK_IMAGE_VIEW_TYPE_2D;
   private boolean depthImage;

   public int getAspectMask() {
      return aspectMask;
   }

   public void setAspectMask(int aspectMask) {
      this.aspectMask = aspectMask;
   }

   public int getBaseArrayLayer() {
      return baseArrayLayer;
   }

   public void setBaseArrayLayer(int baseArrayLayer) {
      this.baseArrayLayer = baseArrayLayer;
   }

   public int getFormat() {
      return format;
   }

   public void setFormat(int format) {
      this.format = format;
   }

   public int getLayerCount() {
      return layerCount;
   }

   public void setLayerCount(int layerCount) {
      this.layerCount = layerCount;
   }

   public int getMipLevels() {
      return mipLevels;
   }

   public void setMipLevels(int mipLevels) {
      this.mipLevels = mipLevels;
   }

   public int getViewType() {
      return viewType;
   }

   public void setViewType(int viewType) {
      this.viewType = viewType;
   }

   public boolean isDepthImage() {
      return depthImage;
   }

   public void setDepthImage(boolean depthImage) {
      this.depthImage = depthImage;
   }
}
