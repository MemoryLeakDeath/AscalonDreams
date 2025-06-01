package tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo;

import org.lwjgl.vulkan.VK14;

public class VulkanImageData {
   private int arrayLayers = 1;
   private int format = VK14.VK_FORMAT_R8G8B8A8_SRGB;
   private int height;
   private int width;
   private int mipLevels = 1;
   private int sampleCount = 1;
   private int usage;

   public int getArrayLayers() {
      return arrayLayers;
   }

   public void setArrayLayers(int arrayLayers) {
      this.arrayLayers = arrayLayers;
   }

   public int getFormat() {
      return format;
   }

   public void setFormat(int format) {
      this.format = format;
   }

   public int getHeight() {
      return height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public int getWidth() {
      return width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public int getMipLevels() {
      return mipLevels;
   }

   public void setMipLevels(int mipLevels) {
      this.mipLevels = mipLevels;
   }

   public int getSampleCount() {
      return sampleCount;
   }

   public void setSampleCount(int sampleCount) {
      this.sampleCount = sampleCount;
   }

   public int getUsage() {
      return usage;
   }

   public void setUsage(int usage) {
      this.usage = usage;
   }

   @Override
   public String toString() {
      return "VulkanImageData{" +
              "arrayLayers=" + arrayLayers +
              ", format=" + format +
              ", height=" + height +
              ", width=" + width +
              ", mipLevels=" + mipLevels +
              ", sampleCount=" + sampleCount +
              ", usage=" + usage +
              '}';
   }
}
