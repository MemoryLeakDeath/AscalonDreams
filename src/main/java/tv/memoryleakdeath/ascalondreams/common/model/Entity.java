package tv.memoryleakdeath.ascalondreams.common.model;

public class Entity {
   private String id;
   private Model model;

   public Entity(String id, Model model) {
      this.id = id;
      this.model = model;
   }

   public String getId() {
      return id;
   }

   public Model getModel() {
      return model;
   }
}
