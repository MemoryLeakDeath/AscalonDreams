package tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMaterial;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMeshData;

import java.util.List;

public class ConvertedModel {
   private List<VulkanMaterial> materials;
   private List<VulkanMeshData> meshData;
   private String texturePath;
   private String id;

   public List<VulkanMaterial> getMaterials() {
      return materials;
   }

   public void setMaterials(List<VulkanMaterial> materials) {
      this.materials = materials;
   }

   public List<VulkanMeshData> getMeshData() {
      return meshData;
   }

   public void setMeshData(List<VulkanMeshData> meshData) {
      this.meshData = meshData;
   }

   public String getTexturePath() {
      return texturePath;
   }

   public void setTexturePath(String texturePath) {
      this.texturePath = texturePath;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   @Override
   public String toString() {
      return "ConvertedModel{" +
              "materials=" + materials +
              ", meshData=" + meshData +
              ", texturePath='" + texturePath + '\'' +
              ", id='" + id + '\'' +
              '}';
   }
}
