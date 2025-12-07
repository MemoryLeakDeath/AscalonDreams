package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;

import java.util.Map;

public record DescriptorPoolInfo(Map<Integer, Integer> descriptorCount, DescriptorPool descriptorPool) {
   public void cleanup(LogicalDevice device) {
      descriptorPool.cleanup(device);
   }

   public boolean isAllAvailable(int[] descriptorTypes, int count) {
      boolean isAvailable = true;
      for(int descriptorType : descriptorTypes) {
         if(!descriptorCount.containsKey(descriptorType)) {
            throw new RuntimeException("Unknown descriptor type: %d".formatted(descriptorType));
         }
         int amountAvailable = descriptorCount.get(descriptorType);
         if(amountAvailable < count) {
            isAvailable = false;
            break;
         }
      }
      return isAvailable;
   }

   public void updateAvailableDescriptorCounts(int[] types, int count) {
      for(int type : types) {
         descriptorCount.put(type, descriptorCount().get(type) - count);
      }
   }

}
