package tv.memoryleakdeath.ascalondreams.vulkan.engine.postprocess;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;

public class EmptyVertexBufferStructure {
   private final VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo;

   public EmptyVertexBufferStructure() {
      this.vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc()
              .sType$Default();
   }

   public void cleanup() {
      vertexInputStateCreateInfo.free();
   }

   public VkPipelineVertexInputStateCreateInfo getVertexInputStateCreateInfo() {
      return vertexInputStateCreateInfo;
   }
}
