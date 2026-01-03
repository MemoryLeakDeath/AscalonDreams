package tv.memoryleakdeath.ascalondreams.animations;

import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.ModelCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.Entity;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationCache {
   private static final Logger logger = LoggerFactory.getLogger(AnimationCache.class);
   private final Map<String, Map<String, VulkanBuffer>> entityAnimationBuffers = new HashMap<>();
   private static AnimationCache animationCache;

   private AnimationCache() {
   }

   public static AnimationCache getInstance() {
      if(animationCache == null) {
         animationCache = new AnimationCache();
      }
      return animationCache;
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      entityAnimationBuffers.values().forEach(m -> m.values().forEach(b -> b.cleanup(device, allocationUtil)));
   }

   public VulkanBuffer getBuffer(String entityId, String meshId) {
      return entityAnimationBuffers.get(entityId).get(meshId);
   }

   public void loadAnimations(LogicalDevice device, MemoryAllocationUtil allocationUtil, Map<String, List<Entity>> entities, ModelCache modelCache) {
      entities.forEach((k,v) -> {
         v.forEach(e -> {
            VulkanModel model = modelCache.getModel(e.getModelId());
            if(model == null) {
               throw new RuntimeException("Model for entity id: %s not found".formatted(e.getModelId()));
            }
            if(model.hasAnimations()) {
               Map<String, VulkanBuffer> bufferMap = new HashMap<>();
               entityAnimationBuffers.put(e.getId(), bufferMap);

               model.getMeshList().forEach(mesh -> {
                  VulkanBuffer animationBuffer = new VulkanBuffer(device, allocationUtil, mesh.vertexBuffer().getRequestedSize(),
                          VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT,
                          Vma.VMA_MEMORY_USAGE_AUTO, 0, 0);
                  bufferMap.put(mesh.id(), animationBuffer);
               });
            }
         });
      });
   }
}
