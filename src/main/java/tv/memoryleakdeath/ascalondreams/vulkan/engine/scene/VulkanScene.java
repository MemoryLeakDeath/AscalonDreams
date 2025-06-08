package tv.memoryleakdeath.ascalondreams.vulkan.engine.scene;

import tv.memoryleakdeath.ascalondreams.common.model.Entity;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanProjection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VulkanScene {
   private Map<String, List<Entity>> entityMap = new HashMap<>();
   private VulkanProjection projection = new VulkanProjection();

   public VulkanScene(int width, int height) {
      projection.resize(width, height);
   }

   public void addEntity(Entity entity) {
      List<Entity> entities = entityMap.getOrDefault(entity.getModelId(), new ArrayList<>());
      entities.add(entity);
      entityMap.put(entity.getModelId(), entities);
   }

   public List<Entity> getEntitiesByModelId(String modelId) {
      return entityMap.getOrDefault(modelId, Collections.emptyList());
   }

   public Entity getEntity(String modelId, String entityId) {
      List<Entity> entityList = getEntitiesByModelId(modelId);
      return entityList.stream().filter(e -> e.getId().equals(entityId))
              .findFirst().orElse(null);
   }

   public void removeAllEntities() {
      entityMap.clear();
   }

   public void removeEntity(Entity entity) {
      List<Entity> entities = entityMap.get(entity.getModelId());
      if (entities != null) {
         entities.removeIf(e -> e.getId().equals(entity.getId()));
      }
   }

   public Map<String, List<Entity>> getEntityMap() {
      return entityMap;
   }

   public VulkanProjection getProjection() {
      return projection;
   }
}
