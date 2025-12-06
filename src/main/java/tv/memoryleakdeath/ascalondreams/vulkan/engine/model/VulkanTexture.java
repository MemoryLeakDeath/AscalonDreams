package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

   public VulkanTexture(LogicalDevice device, String id, ImageSource source, int imageFormat) {
      this.id = id;
      this.width = source.width();
      this.height = source.height();

      createStagingBuffer(device, source.data());
      this.image = new VulkanImage(device, width, height,
              VK13.VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK13.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK13.VK_IMAGE_USAGE_SAMPLED_BIT,
              imageFormat);
      var imageData = new VulkanImageViewData();
      imageData.setAspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT);
      imageData.setFormat(image.getFormat());
      this.imageView = new VulkanImageView(device, image.getId(), imageData, false);
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
            StructureUtils.imageBarrier(stack, cmd.getCommandBuffer(), image.getId(),
                    VK13.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    VK13.VK_PIPELINE_STAGE_TRANSFER_BIT, VK13.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    VK13.VK_ACCESS_TRANSFER_WRITE_BIT, VK13.VK_ACCESS_SHADER_READ_BIT,
                    VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         }
      }
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
}
