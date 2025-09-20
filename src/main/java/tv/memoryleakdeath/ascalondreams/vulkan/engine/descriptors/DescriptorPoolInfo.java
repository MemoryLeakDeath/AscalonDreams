package tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors;

import java.util.List;
import java.util.Map;

public record DescriptorPoolInfo(Map<Integer, Integer> descriptorCount, DescriptorPool pool) {
   public boolean hasAvailableSpace(int spaceRequested, List<Integer> descriptorTypes) {
      // fyi: descriptorCount.get(type) is the amount of available space
      return descriptorTypes.stream().allMatch(type -> descriptorCount.get(type) >= spaceRequested);
   }

   public void adjustAvailableSpace(int spaceConsumed, List<Integer> descriptorTypes) {
      descriptorTypes.forEach(type -> descriptorCount.put(type, descriptorCount.get(type) - spaceConsumed));
   }
}
