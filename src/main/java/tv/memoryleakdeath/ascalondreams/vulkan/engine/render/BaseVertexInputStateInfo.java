package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

public abstract class BaseVertexInputStateInfo {
   private VkPipelineVertexInputStateCreateInfo info;

   public void cleanup() {
      info.free();
   }

   public VkPipelineVertexInputStateCreateInfo getInfo() {
      return info;
   }

   protected void setInfo(VkPipelineVertexInputStateCreateInfo info) {
      this.info = info;
   }
}
