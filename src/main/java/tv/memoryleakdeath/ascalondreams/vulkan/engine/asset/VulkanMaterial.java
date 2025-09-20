package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK14;
import tv.memoryleakdeath.ascalondreams.common.model.Material;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsConstants;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.ByteBuffer;
import java.util.List;

public class VulkanMaterial {
    private static final int MATERIAL_MEM_SIZE = VulkanGraphicsConstants.VEC4_SIZE + VulkanGraphicsConstants.INT_LENGTH * 4;
    private Material material;
    private VulkanBuffer tempSourceBuffer;
    private VulkanBuffer materialBuffer;

    public VulkanMaterial(Material material) {
        this.material = material;
    }

    public void prepareMaterial(LogicalDevice device, VulkanTextureCache cache, VulkanCommandBuffer cmd, List<VulkanTexture> textureList) {
        VulkanTexture texture = cache.createTexture(device, material.getTextureFilePath(), VK14.VK_FORMAT_R8G8B8A8_SRGB);
        texture.recordTextureTransition(cmd);
        textureList.add(texture);
        loadMaterialIntoGpu(device, cmd, cache);
    }

    private void loadMaterialIntoGpu(LogicalDevice device, VulkanCommandBuffer cmd, VulkanTextureCache cache) {
        int bufferSize = MATERIAL_MEM_SIZE;
        tempSourceBuffer = new VulkanBuffer(device, bufferSize, VK14.VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK14.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK14.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        materialBuffer = new VulkanBuffer(device, bufferSize, VK14.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK14.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, VK14.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        long mappedMemory = tempSourceBuffer.map();
        ByteBuffer data = MemoryUtil.memByteBuffer(mappedMemory, (int)tempSourceBuffer.getRequestedSize());
        material.getDiffuseColor().get(0, data);

        // adding this to srcBuffer for some reason?
        data.putInt(VulkanGraphicsConstants.VEC4_SIZE, material.hasTexture() ? 1 : 0);
        data.putInt(VulkanGraphicsConstants.VEC4_SIZE + VulkanGraphicsConstants.INT_LENGTH, cache.getTextureCacheIndex(material.getTextureFilePath()));

        // padding
        data.putInt(VulkanGraphicsConstants.VEC4_SIZE + VulkanGraphicsConstants.INT_LENGTH * 2, 0);
        data.putInt(VulkanGraphicsConstants.VEC4_SIZE + VulkanGraphicsConstants.INT_LENGTH * 3, 0);
        tempSourceBuffer.unmap();

        VulkanUtils.recordTransferCommand(cmd, tempSourceBuffer, materialBuffer);
    }

    public void cleanTempSourceBuffer() {
        if(tempSourceBuffer != null) {
            tempSourceBuffer.cleanup();
        }
    }
}
