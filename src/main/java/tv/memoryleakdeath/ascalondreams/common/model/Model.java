package tv.memoryleakdeath.ascalondreams.common.model;

import java.util.List;

public class Model {
   private String id;
   private List<Mesh> meshes;
   private List<Material> materials;

   public Model(String id, List<Mesh> mesh, List<Material> materials) {
      this.id = id;
      this.meshes = mesh;
      this.materials = materials;
   }

   public String getId() {
      return id;
   }

   public List<Mesh> getMeshes() {
      return meshes;
   }

   public List<Material> getMaterials() {
      return materials;
   }
}
