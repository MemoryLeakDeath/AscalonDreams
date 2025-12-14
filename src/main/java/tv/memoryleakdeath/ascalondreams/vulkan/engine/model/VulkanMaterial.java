package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.nio.ByteBuffer;

public class VulkanMaterial {
   private static final Logger logger = LoggerFactory.getLogger(VulkanMaterial.class);

   private String id;
   private String texturePath;
   private Vector4f diffuseColor;
   private TransferBuffer transferBuffer;

   public VulkanMaterial(String id, String texturePath, Vector4f diffuseColor) {
      this.id = id;
      this.texturePath = texturePath;
      this.diffuseColor = diffuseColor;
   }

   public void load(LogicalDevice device, ByteBuffer dataBuf, int offset, TextureCache cache) {
      if(hasTexture()) {
         cache.addTexture(device, texturePath, texturePath, VK13.VK_FORMAT_R8G8B8A8_SRGB);
      }
      diffuseColor.get(offset, dataBuf);
      dataBuf.putInt(offset + VulkanConstants.VEC4_SIZE, hasTexture() ? 1 : 0);
      dataBuf.putInt(offset + VulkanConstants.VEC4_SIZE + VulkanConstants.INT_SIZE, cache.getPosition(texturePath));

      // padding
      dataBuf.putInt(offset + VulkanConstants.VEC4_SIZE + VulkanConstants.INT_SIZE * 2, 0);
      dataBuf.putInt(offset + VulkanConstants.VEC4_SIZE + VulkanConstants.INT_SIZE * 3, 0);
   }

   public boolean hasTexture() {
      return StringUtils.isNotBlank(texturePath);
   }

   public String getId() {
      return id;
   }

   public String getTexturePath() {
      return texturePath;
   }

   public Vector4f getDiffuseColor() {
      return diffuseColor;
   }

   public TransferBuffer getTransferBuffer() {
      return transferBuffer;
   }
}
