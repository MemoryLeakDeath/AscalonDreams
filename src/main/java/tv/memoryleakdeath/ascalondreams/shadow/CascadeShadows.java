package tv.memoryleakdeath.ascalondreams.shadow;

import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;

import java.util.ArrayList;
import java.util.List;

public class CascadeShadows {
   private final List<CascadeData> cascadeData = new ArrayList<>();

   public CascadeShadows() {
      for(int i = 0; i < VulkanScene.SHADOW_MAP_CASCADE_COUNT; i++) {
         cascadeData.add(new CascadeData());
      }
   }

   public List<CascadeData> getCascadeData() {
      return cascadeData;
   }
}
