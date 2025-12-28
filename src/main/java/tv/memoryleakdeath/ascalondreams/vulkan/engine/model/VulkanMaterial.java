package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

import java.nio.ByteBuffer;

public class VulkanMaterial {
   private static final Logger logger = LoggerFactory.getLogger(VulkanMaterial.class);

   private String id;
   private String texturePath;
   private Vector4f diffuseColor;
   private String normalMapPath;
   private String roughMapPath;
   private float roughnessFactor;
   private float metallicFactor;

   @JsonIgnore
   private boolean transparent;

   @JsonIgnore
   private TransferBuffer transferBuffer;

   public VulkanMaterial() {
   }

   public VulkanMaterial(String id, String texturePath, Vector4f diffuseColor, String normalMapPath, String roughMapPath,
                         float roughnessFactor, float metallicFactor) {
      this.id = id;
      this.texturePath = texturePath;
      this.diffuseColor = diffuseColor;
      this.normalMapPath = normalMapPath;
      this.roughMapPath = roughMapPath;
      this.roughnessFactor = roughnessFactor;
      this.metallicFactor = metallicFactor;
   }

   public int load(LogicalDevice device, MemoryAllocationUtil allocationUtil, ByteBuffer dataBuf, int offset, TextureCache cache) {
      if(hasTexture()) {
         VulkanTexture texture = cache.addTexture(device, allocationUtil, texturePath, texturePath, VK13.VK_FORMAT_R8G8B8A8_SRGB);
         transparent = texture.isTransparent();
      } else {
         transparent = diffuseColor.w < 1.0f;
      }
      diffuseColor.get(offset, dataBuf);
      offset += VulkanConstants.VEC4_SIZE;
      dataBuf.putInt(offset, hasTexture() ? 1 : 0);
      offset += VulkanConstants.INT_SIZE;
      dataBuf.putInt(offset, cache.getPosition(texturePath));
      offset += VulkanConstants.INT_SIZE;

      if(hasNormalMap()) {
         cache.addTexture(device, allocationUtil, normalMapPath, normalMapPath, VK13.VK_FORMAT_R8G8B8A8_UNORM);
      }
      dataBuf.putInt(offset, hasNormalMap() ? 1 : 0);
      offset += VulkanConstants.INT_SIZE;
      dataBuf.putInt(offset, cache.getPosition(normalMapPath));
      offset += VulkanConstants.INT_SIZE;

      if(hasRoughMap()) {
         cache.addTexture(device, allocationUtil, roughMapPath, roughMapPath, VK13.VK_FORMAT_R8G8B8A8_UNORM);
      }
      dataBuf.putInt(offset, hasRoughMap() ? 1 : 0);
      offset += VulkanConstants.INT_SIZE;
      dataBuf.putInt(offset, cache.getPosition(roughMapPath));
      offset += VulkanConstants.INT_SIZE;

      dataBuf.putFloat(offset, roughnessFactor);
      offset += VulkanConstants.FLOAT_SIZE;
      dataBuf.putFloat(offset, metallicFactor);
      offset += VulkanConstants.FLOAT_SIZE;

      return offset;
   }

   public boolean hasTexture() {
      return StringUtils.isNotBlank(texturePath);
   }

   public boolean hasNormalMap() {
      return StringUtils.isNotBlank(normalMapPath);
   }

   public boolean hasRoughMap() {
      return StringUtils.isNotBlank(roughMapPath);
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

   public void setId(String id) {
      this.id = id;
   }

   public void setTexturePath(String texturePath) {
      this.texturePath = texturePath;
   }

   public void setDiffuseColor(Vector4f diffuseColor) {
      this.diffuseColor = diffuseColor;
   }

   public boolean isTransparent() {
      return transparent;
   }

   public void setTransparent(boolean transparent) {
      this.transparent = transparent;
   }

   public String getNormalMapPath() {
      return normalMapPath;
   }

   public void setNormalMapPath(String normalMapPath) {
      this.normalMapPath = normalMapPath;
   }

   public String getRoughMapPath() {
      return roughMapPath;
   }

   public void setRoughMapPath(String roughMapPath) {
      this.roughMapPath = roughMapPath;
   }

   public float getRoughnessFactor() {
      return roughnessFactor;
   }

   public void setRoughnessFactor(float roughnessFactor) {
      this.roughnessFactor = roughnessFactor;
   }

   public float getMetallicFactor() {
      return metallicFactor;
   }

   public void setMetallicFactor(float metallicFactor) {
      this.metallicFactor = metallicFactor;
   }

   @Override
   public String toString() {
      return "VulkanMaterial{" +
              "id='" + id + '\'' +
              ", texturePath='" + texturePath + '\'' +
              ", diffuseColor=" + diffuseColor +
              ", transparent=" + transparent +
              ", transferBuffer=" + transferBuffer +
              '}';
   }
}
