package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;

public class VulkanRenderer {
   private final VulkanRenderInstance instance;
   private VulkanWindow window;

   public VulkanRenderer(VulkanWindow window) {
      this.instance = new VulkanRenderInstance(false);
      this.window = window;
   }

   public void cleanup() {
      instance.cleanup();
   }

   public void render() {
      // todo implement
   }
}
