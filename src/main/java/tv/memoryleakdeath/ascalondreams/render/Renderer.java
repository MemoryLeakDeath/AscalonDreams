package tv.memoryleakdeath.ascalondreams.render;

import org.apache.commons.lang3.NotImplementedException;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;

public interface Renderer {
   static <T> T getInstance() {
      throw new NotImplementedException("Must implement this method!");
   }

   void cleanup();
   void load();
   void render(CommandBuffer commandBuffer, int currentFrame, int imageIndex);
   void resize();
}
