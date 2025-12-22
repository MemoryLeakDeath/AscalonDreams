package tv.memoryleakdeath.ascalondreams.vulkan.engine.scene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.camera.Camera;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;

import java.util.ArrayList;
import java.util.List;

public class VulkanScene {
   private static final Logger logger = LoggerFactory.getLogger(VulkanScene.class);

   private final List<Entity> entities = new ArrayList<>();
   private final Projection projection;
   private final Camera camera;

   public VulkanScene(VulkanWindow window) {
      this.projection = new Projection(window.getFov(), window.getZNear(), window.getZFar(), window.getWidth(), window.getHeight());
      this.camera = new Camera();
   }

   public void addEntity(Entity entity) {
      entities.add(entity);
   }

   public void removeAllEntities() {
      entities.clear();
   }

   public void removeEntity(Entity entity) {
      entities.removeIf(e -> e.getId().equals(entity.getId()));
   }

   public List<Entity> getEntities() {
      return entities;
   }

   public Projection getProjection() {
      return projection;
   }

   public Camera getCamera() {
      return camera;
   }
}
