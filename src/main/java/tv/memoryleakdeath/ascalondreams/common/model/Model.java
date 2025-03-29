package tv.memoryleakdeath.ascalondreams.common.model;

import java.util.List;

public class Model {
   private String id;
   private Mesh mesh;
   private List<Material> materials;

   public Model(String id, Mesh mesh, List<Material> materials) {
      this.id = id;
      this.mesh = mesh;
      this.materials = materials;
   }

   public String getId() {
      return id;
   }

   public Mesh getMesh() {
      return mesh;
   }

   public List<Material> getMaterials() {
      return materials;
   }
}
