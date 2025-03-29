package tv.memoryleakdeath.ascalondreams.vulkan.engine.scene;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.MeshData;

import java.util.ArrayList;
import java.util.List;

public class VulkanModelData {
   private String id;
   private List<MeshData> meshData = new ArrayList<>();

   public VulkanModelData(String id, List<MeshData> meshData) {
      this.id = id;
      this.meshData = meshData;
   }

   public String getId() {
      return id;
   }

   public List<MeshData> getMeshData() {
      return meshData;
   }
}
