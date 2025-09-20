package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.common.asset.BaseTexture;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImage;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.nio.ByteBuffer;

public class VulkanTexture extends BaseTexture {
    private static final Logger logger = LoggerFactory.getLogger(VulkanTexture.class);
    private VulkanImage image;
    private VulkanImageView imageView;
    private int mipLevels;
    private boolean recordedTransition = false;
    private VulkanBuffer stagingBuffer;
    private LogicalDevice device;
    private int imageFormat;

    public VulkanTexture(LogicalDevice device, String fileName, int imageFormat) {
        logger.debug("Processing texture: {}", fileName);
        this.fileName = fileName;
        this.device = device;
        this.imageFormat = imageFormat;
        loadTexture();
    }

    @Override
    protected void generateTexture(int width, int height, ByteBuffer buffer, int mipLevels) {
        this.mipLevels = mipLevels;
        createStagingBuffer(buffer);
        VulkanImageData imageData = new VulkanImageData();
        imageData.setFormat(imageFormat);
        imageData.setHeight(height);
        imageData.setUsage(VK14.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK14.VK_IMAGE_USAGE_SAMPLED_BIT);
        imageData.setWidth(width);
        imageData.setMipLevels(mipLevels);
        this.image = new VulkanImage(device, imageData);
        VulkanImageViewData imageViewData = new VulkanImageViewData();
        imageViewData.setFormat(image.getImageData().getFormat());
        imageViewData.setMipLevels(mipLevels);
        imageViewData.setAspectMask(VK14.VK_IMAGE_ASPECT_COLOR_BIT);
        this.imageView = new VulkanImageView(device, image.getImageId(), imageViewData);
    }

    @Override
    public void cleanup() {
        cleanupStagingBuffer();
        imageView.cleanup();
        image.cleanup();
    }

    public void cleanupStagingBuffer() {
        if(stagingBuffer != null) {
            stagingBuffer.cleanup();
            stagingBuffer = null;
        }
    }

    private void createStagingBuffer(ByteBuffer data) {
        int size = data.remaining();
        this.stagingBuffer = new VulkanBuffer(device, size, VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK14.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK14.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        long mappedMemory = stagingBuffer.map();
        ByteBuffer buf = MemoryUtil.memByteBuffer(mappedMemory, (int) stagingBuffer.getRequestedSize());
        buf.put(data);
        data.flip();
        stagingBuffer.unmap();
    }

    public VulkanImageView getImageView() {
        return imageView;
    }

    public void recordTextureTransition(VulkanCommandBuffer cmd) {
        if(stagingBuffer != null && !recordedTransition) {
            logger.debug("Recording texture transition for file: {}", fileName);
            recordedTransition = true;
            try(MemoryStack stack = MemoryStack.stackPush()) {
                StructureUtils.recordImageTransition(stack, cmd, VK14.VK_IMAGE_LAYOUT_UNDEFINED, VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, image, mipLevels);
                StructureUtils.recordCopyBuffer(stack, cmd, stagingBuffer, image);
                StructureUtils.recordImageTransition(stack, cmd, VK14.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK14.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, image, mipLevels);
            }
        } else {
            logger.debug("Texture transition already completed for: {}", fileName);
        }
    }
}
