package tv.memoryleakdeath.ascalondreams.vulkan.engine.scene;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.camera.Camera;
import tv.memoryleakdeath.ascalondreams.lighting.Light;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;

import java.util.ArrayList;
import java.util.List;

public class VulkanScene {
   private static final Logger logger = LoggerFactory.getLogger(VulkanScene.class);

   public static final int MAX_LIGHTS = 10;
   public static final int SHADOW_MAP_CASCADE_COUNT = 3;

   private final List<Entity> entities = new ArrayList<>();
   private final Projection projection;
   private final Camera camera;
   private final Vector3f ambientLightColor = new Vector3f();
   private float ambientLightIntensity = 0.0f;
   private List<Light> lights;

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

   public Vector3f getAmbientLightColor() {
      return ambientLightColor;
   }

   public float getAmbientLightIntensity() {
      return ambientLightIntensity;
   }

   public void setAmbientLightIntensity(float ambientLightIntensity) {
      this.ambientLightIntensity = ambientLightIntensity;
   }

   public List<Light> getLights() {
      return lights;
   }

   public void setLights(List<Light> lights) {
      if(lights != null && lights.size() > MAX_LIGHTS) {
         throw new RuntimeException("Maximum number of scene lights exceeded! Max: %d Requested: %d".formatted(MAX_LIGHTS, lights.size()));
      }
      this.lights = lights;
   }
}
