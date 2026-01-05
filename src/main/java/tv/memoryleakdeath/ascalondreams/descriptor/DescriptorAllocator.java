package tv.memoryleakdeath.ascalondreams.descriptor;

import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.device.PhysicalDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DescriptorAllocator {
   private static final Logger logger = LoggerFactory.getLogger(DescriptorAllocator.class);
   private static final int MAX_DESCRIPTORS = 1000;

   private final Map<Integer, Integer> descriptorLimits;
   private final List<DescriptorPoolInfo> descriptorPoolList = new ArrayList<>();
   private final Map<String, DescriptorSetInfo> descriptorSetInfoMap = new HashMap<>();
   private static DescriptorAllocator descriptorAllocator;

   private DescriptorAllocator() {
      logger.debug("Creating descriptor allocator...");
      LogicalDevice device = DeviceManager.getDevice();
      this.descriptorLimits = initDescriptorLimits(device.getPhysicalDevice());
      descriptorPoolList.add(initDescriptorPoolInfo(device, descriptorLimits));
   }

   public static DescriptorAllocator getInstance() {
      if(descriptorAllocator == null) {
         descriptorAllocator = new DescriptorAllocator();
      }
      return descriptorAllocator;
   }

   private Map<Integer, Integer> initDescriptorLimits(PhysicalDevice physicalDevice) {
      var limits = physicalDevice.getDeviceProperties().properties().limits();
      Map<Integer, Integer> descriptorLimits = new HashMap<>();
      descriptorLimits.put(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, Math.min(MAX_DESCRIPTORS, limits.maxDescriptorSetUniformBuffers()));
      descriptorLimits.put(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, Math.min(MAX_DESCRIPTORS, limits.maxDescriptorSetSamplers()));
      descriptorLimits.put(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, Math.min(MAX_DESCRIPTORS, limits.maxDescriptorSetStorageBuffers()));
      return descriptorLimits;
   }

   private DescriptorPoolInfo initDescriptorPoolInfo(LogicalDevice device, Map<Integer, Integer> descriptorLimits) {
      List<DescriptorTypeCount> typeCounts = descriptorLimits.entrySet().stream()
              .map(e -> new DescriptorTypeCount(e.getKey(), e.getValue())).toList();
      var descriptorPool = new DescriptorPool(device, typeCounts);
      return new DescriptorPoolInfo(new HashMap<>(descriptorLimits), descriptorPool);
   }

   public synchronized DescriptorSet addDescriptorSet(LogicalDevice device, String id, DescriptorSetLayout layout) {
      return addDescriptorSets(device, id, 1, layout).getFirst();
   }

   public synchronized List<DescriptorSet> addDescriptorSets(LogicalDevice device, String id, int count, DescriptorSetLayout layout) {
      // check for room for new sets
      DescriptorPoolInfo targetPool = null;
      int poolPosition = 0;
      int[] types = layout.getLayoutInfoTypes();
      for(DescriptorPoolInfo info : descriptorPoolList) {
         checkUnderMaxTotal(types, count);
         if(info.isAllAvailable(types, count)) {
            targetPool = info;
            break;
         }
         poolPosition++;
      }

      if(targetPool == null) {
         targetPool = initDescriptorPoolInfo(device, descriptorLimits);
         descriptorPoolList.add(targetPool);
         poolPosition++;
      }

      List<DescriptorSet> descriptorSetList = new ArrayList<>();
      for(int i = 0; i < count; i++) {
         descriptorSetList.add(new DescriptorSet(device, targetPool.descriptorPool(), layout));
      }
      descriptorSetInfoMap.put(id, new DescriptorSetInfo(descriptorSetList, poolPosition));
      // update consumed descriptors
      targetPool.updateAvailableDescriptorCounts(types, count);
      return descriptorSetList;
   }

   private void checkUnderMaxTotal(int[] descriptorTypes, int count) {
      for(int type : descriptorTypes) {
         int max = descriptorLimits.getOrDefault(type, 0);
         if(count > max) {
            throw new RuntimeException("Cannot create more than %d for descriptor type: %d".formatted(max, type));
         }
      }
   }

   public synchronized void cleanup(LogicalDevice device) {
      logger.debug("Cleaning up descriptor allocator");
      descriptorSetInfoMap.clear();
      descriptorPoolList.forEach(d -> d.cleanup(device));
   }

   public synchronized void freeDescriptorSet(LogicalDevice device, String id) {
      DescriptorSetInfo info = descriptorSetInfoMap.get(id);
      if(info == null) {
         logger.info("Could not find descriptor set with id: {}", id);
         return;
      }
      if(info.poolPosition() >= descriptorPoolList.size()) {
         logger.info("Could not find descriptor pool with id: {}", id);
         return;
      }
      DescriptorPoolInfo poolInfo = descriptorPoolList.get(info.poolPosition());
      info.descriptorSets().forEach(set -> poolInfo.descriptorPool().freeDescriptorSet(device, set.getId()));
   }

   public synchronized DescriptorSet getDescriptorSet(String id) {
      return getDescriptorSet(id, 0);
   }

   public synchronized DescriptorSet getDescriptorSet(String id, int position) {
      if(descriptorSetInfoMap.containsKey(id)) {
         return descriptorSetInfoMap.get(id).descriptorSets().get(position);
      }
      return null;
   }
}
