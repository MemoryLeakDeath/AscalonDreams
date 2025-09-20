package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors;

import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkPhysicalDeviceLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DescriptorAllocator {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorAllocator.class);
   public static final int MAX_DESCRIPTORS = 1000;

   private Map<Integer, Integer> defaultDescriptorLimits;
   private List<DescriptorPoolInfo> descriptorPoolInfoList = new ArrayList<>();
   private Map<String, DescriptorSetInfo> descriptorSetInfoMap = new HashMap<>();

   public DescriptorAllocator(LogicalDevice device) {
      this.defaultDescriptorLimits = createDefaultDescriptorLimits(device.getPhysicalDevice());
      descriptorPoolInfoList.add(createPoolInfo(device));
   }

   private Map<Integer, Integer> createDefaultDescriptorLimits(PhysicalDevice physicalDevice) {
      VkPhysicalDeviceLimits deviceLimits = physicalDevice.getDeviceProperties().limits();
      Map<Integer, Integer> limitsMap = new HashMap<>();
      limitsMap.put(VK14.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, Math.min(MAX_DESCRIPTORS, deviceLimits.maxDescriptorSetUniformBuffers()));
      limitsMap.put(VK14.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, Math.min(MAX_DESCRIPTORS, deviceLimits.maxDescriptorSetSamplers()));
      limitsMap.put(VK14.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, Math.min(MAX_DESCRIPTORS, deviceLimits.maxDescriptorSetStorageBuffers()));
      return limitsMap;
   }

   private DescriptorPoolInfo createPoolInfo(LogicalDevice device) {
      Map<Integer, Integer> defaultCountsCopy = new HashMap<>(defaultDescriptorLimits);
      List<DescriptorTypeCount> typeCounts = defaultCountsCopy.entrySet().stream()
              .map(e -> new DescriptorTypeCount(e.getKey(), e.getValue())).toList();
      return new DescriptorPoolInfo(defaultCountsCopy, new DescriptorPool(device, typeCounts));
   }

   public synchronized List<DescriptorSet> addDescriptorSets(LogicalDevice device, String id, int count, DescriptorSetLayout layout) {
      // sanity check, make sure descriptor types passed in exist in our defaults
      List<Integer> descriptorTypes = layout.getLayoutInfoDescriptorTypes();
      validateLimitsAndRequestedCount(count, descriptorTypes);

      Object[] results = findOrCreatePoolWithEnoughSpace(device, count, descriptorTypes);
      DescriptorPoolInfo targetPool = (DescriptorPoolInfo) results[0];
      int poolPosition = (int)results[1];

      List<DescriptorSet> newDescriptorSets = new ArrayList<>();
      for(int i = 0; i < count; i++) {
         newDescriptorSets.add(new DescriptorSet(device, targetPool.pool(), layout));
      }
      descriptorSetInfoMap.put(id, new DescriptorSetInfo(newDescriptorSets, poolPosition));
      targetPool.adjustAvailableSpace(count, descriptorTypes);
      return newDescriptorSets;
   }

   private void validateLimitsAndRequestedCount(int count, List<Integer> descriptorTypes) {
      // check descriptor types exist
      if(!descriptorTypesExist(descriptorTypes)) {
         throw new RuntimeException("Unknown descriptor type!");
      }

      // check count doesn't exceed max default limits
      if(requestedCountExceedsLimits(count, descriptorTypes)) {
         throw new RuntimeException("Attempted to create more descriptor sets than allowed!");
      }
   }

   private boolean descriptorTypesExist(List<Integer> descriptorTypes) {
      return descriptorTypes.stream()
              .allMatch(type -> {
                 boolean exists = defaultDescriptorLimits.containsKey(type);
                 if(!exists) {
                    logger.error("Unknown type: [{}]", type);
                 }
                 return exists;
              });
   }

   private boolean requestedCountExceedsLimits(final int count, List<Integer> descriptorTypes) {
      return descriptorTypes.stream().anyMatch(type -> {
         int limit = defaultDescriptorLimits.getOrDefault(type, -1);
         boolean matches = (count > limit);
         if(matches) {
            logger.error("Cannot create more than {} for descriptor type {}; requested: {}", limit, type, count);
         }
         return matches;
      });
   }

   private Object[] findOrCreatePoolWithEnoughSpace(LogicalDevice device, final int count, List<Integer> descriptorTypes) {
      int poolPosition = 0;
      DescriptorPoolInfo targetPoolInfo = null;
      for(DescriptorPoolInfo poolInfo : descriptorPoolInfoList) {
         if(poolInfo.hasAvailableSpace(count, descriptorTypes)) {
            targetPoolInfo = poolInfo;
         }
         poolPosition++;
      }
      if(targetPoolInfo == null) {
         // create a new pool to facilitate the request
         targetPoolInfo = createPoolInfo(device);
         descriptorPoolInfoList.add(targetPoolInfo);
         poolPosition++;
      }
      return new Object[] {targetPoolInfo, poolPosition};
   }

   public synchronized void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up descriptor allocator");
      descriptorSetInfoMap.clear();
      descriptorPoolInfoList.forEach(d -> d.pool().cleanup(device));
   }

   public synchronized void freeDescriptorSet(LogicalDevice device, String id) {
      DescriptorSetInfo info = descriptorSetInfoMap.get(id);
      if(info == null) {
         logger.error("Cannot free descriptor set; set: {} not found!", id);
         return;
      }
      if(info.poolPosition() >= descriptorPoolInfoList.size()) {
         logger.error("Could not find descriptor pool associated with set: {}", id);
         return;
      }
      DescriptorPoolInfo pool = descriptorPoolInfoList.get(info.poolPosition());
      info.setList().forEach(d -> pool.pool().freeDescriptorSet(device, d.getId()));
   }

   public synchronized DescriptorSet getDescriptorSet(String id, int pos) {
      DescriptorSet descriptorSet = null;
      if(descriptorSetInfoMap.containsKey(id)) {
         descriptorSet = descriptorSetInfoMap.get(id).setList().get(pos);
      }
      return descriptorSet;
   }

   public synchronized DescriptorSet getDescriptorSet(String id) {
      return getDescriptorSet(id, 0);
   }
}
