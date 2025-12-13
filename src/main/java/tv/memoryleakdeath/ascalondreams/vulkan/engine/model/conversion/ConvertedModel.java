package tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMaterial;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMeshData;

import java.util.List;

public class ConvertedModel {
   private int indexOffset;
   private int vertexOffset;
   private List<VulkanMaterial> materials;
   private List<VulkanMeshData> meshData;
   private String texturePath;

   public int getIndexOffset() {
      return indexOffset;
   }

   public void setIndexOffset(int indexOffset) {
      this.indexOffset = indexOffset;
   }

   public int getVertexOffset() {
      return vertexOffset;
   }

   public void setVertexOffset(int vertexOffset) {
      this.vertexOffset = vertexOffset;
   }

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

   @Override
   public String toString() {
      return "ConvertedModel{" +
              "indexOffset=" + indexOffset +
              ", vertexOffset=" + vertexOffset +
              ", materials=" + materials +
              ", meshData=" + meshData +
              ", texturePath='" + texturePath + '\'' +
              '}';
   }
}
