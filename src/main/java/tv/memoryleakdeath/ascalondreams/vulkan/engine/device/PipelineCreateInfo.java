package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.BaseVertexInputStateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.VulkanShaderProgram;

public record PipelineCreateInfo(long renderPass, VulkanShaderProgram shaderProgram, int numColorAttachments,
                                 BaseVertexInputStateInfo vertexInputStateInfo) {
   public void cleanup() {
      vertexInputStateInfo.cleanup();
   }
}
