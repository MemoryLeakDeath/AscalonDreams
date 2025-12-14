package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import java.util.Arrays;

public class VulkanMeshData {
   private float[] verticies;
   private float[] textureCoords;
   private int[] indicies;
   private String materialId;
   private String id;

   public float[] getVerticies() {
      return verticies;
   }

   public void setVerticies(float[] verticies) {
      this.verticies = verticies;
   }

   public float[] getTextureCoords() {
      return textureCoords;
   }

   public void setTextureCoords(float[] textureCoords) {
      this.textureCoords = textureCoords;
   }

   public int[] getIndicies() {
      return indicies;
   }

   public void setIndicies(int[] indicies) {
      this.indicies = indicies;
   }

   public String getMaterialId() {
      return materialId;
   }

   public void setMaterialId(String materialId) {
      this.materialId = materialId;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   @Override
   public String toString() {
      return "VulkanMeshData{" +
              "verticies=" + Arrays.toString(verticies) +
              ", textureCoords=" + Arrays.toString(textureCoords) +
              ", indicies=" + Arrays.toString(indicies) +
              ", materialId='" + materialId + '\'' +
              ", id='" + id + '\'' +
              '}';
   }
}
