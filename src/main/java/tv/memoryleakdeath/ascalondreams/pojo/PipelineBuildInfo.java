package tv.memoryleakdeath.ascalondreams.pojo;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderModule;

import java.nio.ByteBuffer;
import java.util.List;

public record PipelineBuildInfo(List<ShaderModule> shaderModules, VkPipelineVertexInputStateCreateInfo info, int[] colorFormats,
                                int depthFormat, List<PushConstantRange> pushConstantRanges, List<DescriptorSetLayout> descriptorSetLayouts,
                                boolean useBlend, boolean depthClamp) {
   public VkPipelineShaderStageCreateInfo.Buffer createShaderStages(MemoryStack stack) {
      int numModules = shaderModules.size();
      ByteBuffer main = stack.UTF8("main");
      var info = VkPipelineShaderStageCreateInfo.calloc(numModules, stack);
      for(var i = 0; i < numModules; i++) {
         ShaderModule module = shaderModules.get(i);
         info.get(i)
                 .sType$Default()
                 .stage(module.getStage())
                 .module(module.getId())
                 .pName(main);
         if(module.getSpecializationInfo() != null) {
            info.get(i).pSpecializationInfo(module.getSpecializationInfo());
         }
      }
      return info;
   }
}
