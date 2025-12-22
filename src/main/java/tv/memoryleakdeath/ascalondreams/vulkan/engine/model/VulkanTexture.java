package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkOffset3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.util.MathUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.ImageSource;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImage;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.nio.ByteBuffer;

public class VulkanTexture {
   private static final Logger logger = LoggerFactory.getLogger(VulkanTexture.class);
   private final String id;
   private final VulkanImage image;
   private final VulkanImageView imageView;
   private final int height;
   private final int width;
   private boolean recordedTransition = false;
   private VulkanBuffer stagingBuffer;
   private boolean transparent = false;

   public VulkanTexture(LogicalDevice device, String id, ImageSource source, int imageFormat) {
      this.id = id;
      this.width = source.width();
      this.height = source.height();

      calculateTransparency(source.data());
      createStagingBuffer(device, source.data());
      int mipLevels = MathUtils.calculateMipLevels(width, height);
      this.image = new VulkanImage(device, width, height,
              VK13.VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK13.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK13.VK_IMAGE_USAGE_SAMPLED_BIT,
              imageFormat, mipLevels);
      var imageData = new VulkanImageViewData();
      imageData.setAspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT);
      imageData.setFormat(image.getFormat());
      imageData.setMipLevels(mipLevels);
      this.imageView = new VulkanImageView(device, image.getId(), imageData, false);
   }

   private void calculateTransparency(ByteBuffer data) {
      int numPixels = data.capacity() / 4;
      int offset = 0;
      for(int i = 0; i < numPixels; i++) {
         int a = (0xFF & data.get(offset + 3));
         if(a < 255) {
            transparent = true;
            break;
         }
         offset += 4;
      }
   }

   public void cleanup(LogicalDevice device) {
      cleanupStagingBuffer(device);
      imageView.cleanup();
      image.cleanup(device);
   }

   public void cleanupStagingBuffer(LogicalDevice device) {
      if(stagingBuffer != null) {
         stagingBuffer.cleanup(device);
         stagingBuffer = null;
      }
   }

   private void createStagingBuffer(LogicalDevice device, ByteBuffer data) {
      int size = data.remaining();
      this.stagingBuffer = new VulkanBuffer(device, size, VK13.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
              VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK13.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
      long mappedMemory = stagingBuffer.map(device);
      ByteBuffer buf = MemoryUtil.memByteBuffer(mappedMemory, (int) stagingBuffer.getRequestedSize());
      buf.put(data);
      data.flip();

      stagingBuffer.unMap(device);
   }

   private void recordCopyBuffer(MemoryStack stack, CommandBuffer cmd, VulkanBuffer data) {
      VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
              .bufferOffset(0)
              .bufferRowLength(0)
              .bufferImageHeight(0)
              .imageSubresource(layer -> layer.aspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT)
                      .mipLevel(0)
                      .baseArrayLayer(0)
                      .layerCount(1))
              .imageOffset(offset -> offset.x(0).y(0).z(0))
              .imageExtent(extent -> extent.width(width).height(height).depth(1));
      VK13.vkCmdCopyBufferToImage(cmd.getCommandBuffer(), data.getBuffer(), image.getId(), VK13.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
   }

   public void recordTextureTransition(CommandBuffer cmd) {
      if(stagingBuffer != null && !recordedTransition) {
         recordedTransition = true;
         try(var stack = MemoryStack.stackPush()) {
            StructureUtils.imageBarrier(stack, cmd.getCommandBuffer(), image.getId(),
                    VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK13.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK13.VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK13.VK_IMAGE_ASPECT_COLOR_BIT);
            recordCopyBuffer(stack, cmd, stagingBuffer);
            recordGenerateMipMaps(stack, cmd);
         }
      }
   }

   private void recordGenerateMipMaps(MemoryStack stack, CommandBuffer cmd) {
      var subResourceRange = VkImageSubresourceRange.calloc(stack)
              .aspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT)
              .baseArrayLayer(0)
              .levelCount(1)
              .layerCount(1);
      var barrier = VkImageMemoryBarrier2.calloc(1, stack)
              .sType$Default()
              .image(image.getId())
              .srcQueueFamilyIndex(VK13.VK_QUEUE_FAMILY_IGNORED)
              .dstQueueFamilyIndex(VK13.VK_QUEUE_FAMILY_IGNORED)
              .subresourceRange(subResourceRange);

      var depInfo = VkDependencyInfo.calloc(stack)
              .sType$Default()
              .pImageMemoryBarriers(barrier);

      int mipWidth = width;
      int mipHeight = height;
      int mipLevels = image.getMipLevels();
      for(int i = 1; i < mipLevels; i++) {
         subResourceRange.baseMipLevel(i - 1);
         barrier.subresourceRange(subResourceRange)
                 .oldLayout(VK13.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                 .newLayout(VK13.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                 .srcStageMask(VK13.VK_PIPELINE_STAGE_TRANSFER_BIT)
                 .dstStageMask(VK13.VK_PIPELINE_STAGE_TRANSFER_BIT)
                 .srcAccessMask(VK13.VK_ACCESS_TRANSFER_WRITE_BIT)
                 .dstAccessMask(VK13.VK_ACCESS_TRANSFER_READ_BIT);
         VK13.vkCmdPipelineBarrier2(cmd.getCommandBuffer(), depInfo);

         int auxi = i;
         VkOffset3D srcOffset0 = VkOffset3D.calloc(stack).x(0).y(0).z(0);
         VkOffset3D srcOffset1 = VkOffset3D.calloc(stack).x(mipWidth).y(mipHeight).z(1);
         VkOffset3D dstOffset0 = VkOffset3D.calloc(stack).x(0).y(0).z(0);
         VkOffset3D dstOffset1 = VkOffset3D.calloc(stack)
                 .x(mipWidth > 1 ? mipWidth / 2 : 1)
                 .y(mipHeight > 1 ? mipHeight / 2 : 1)
                 .z(0);

         VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack)
                 .srcOffsets(0, srcOffset0)
                 .srcOffsets(1, srcOffset1)
                 .srcSubresource(it -> it
                         .aspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT)
                         .mipLevel(auxi - 1)
                         .baseArrayLayer(0)
                         .layerCount(1))
                 .dstOffsets(0, dstOffset0)
                 .dstOffsets(1, dstOffset1)
                 .dstSubresource(it -> it
                         .aspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT)
                         .mipLevel(auxi)
                         .baseArrayLayer(0)
                         .layerCount(1));
         VK13.vkCmdBlitImage(cmd.getCommandBuffer(), image.getId(), VK13.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                 image.getId(), VK13.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, blit, VK13.VK_FILTER_LINEAR);

         barrier.oldLayout(VK13.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                 .newLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                 .srcAccessMask(VK13.VK_ACCESS_TRANSFER_READ_BIT)
                 .dstAccessMask(VK13.VK_ACCESS_SHADER_READ_BIT);
         barrier.srcStageMask(VK13.VK_PIPELINE_STAGE_TRANSFER_BIT)
                 .dstStageMask(VK13.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT);

         VK13.vkCmdPipelineBarrier2(cmd.getCommandBuffer(), depInfo);

         if(mipWidth > 1) {
            mipWidth /= 2;
         }
         if(mipHeight > 1) {
            mipHeight /= 2;
         }
      }

      barrier.subresourceRange(it -> it
              .baseMipLevel(mipLevels - 1))
              .oldLayout(VK13.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
              .newLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
              .srcStageMask(VK13.VK_PIPELINE_STAGE_TRANSFER_BIT)
              .dstStageMask(VK13.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
              .srcAccessMask(VK13.VK_ACCESS_TRANSFER_WRITE_BIT)
              .dstAccessMask(VK13.VK_ACCESS_SHADER_READ_BIT);

      VK13.vkCmdPipelineBarrier2(cmd.getCommandBuffer(), depInfo);
   }

   public String getId() {
      return id;
   }

   public VulkanImageView getImageView() {
      return imageView;
   }

   public int getHeight() {
      return height;
   }

   public int getWidth() {
      return width;
   }

   public boolean isTransparent() {
      return transparent;
   }

   public void setTransparent(boolean transparent) {
      this.transparent = transparent;
   }
}
