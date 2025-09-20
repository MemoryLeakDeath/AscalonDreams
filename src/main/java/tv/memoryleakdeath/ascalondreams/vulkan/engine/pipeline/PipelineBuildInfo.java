package tv.memoryleakdeath.ascalondreams.vulkan.engine.pipeline;

import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.VulkanShaderProgram;

import java.util.ArrayList;
import java.util.List;

public class PipelineBuildInfo {
   private int colorFormat;
   private List<ShaderModule> shaderModules;
   private VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo;
   private int depthFormat = VK14.VK_FORMAT_UNDEFINED;
   private List<DescriptorSetLayout> descriptorSetLayouts = new ArrayList<>();
   private List<PushConstantRange> pushConstantRanges = new ArrayList<>();

   public PipelineBuildInfo(int colorFormat, List<ShaderModule> shaderModules, VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo) {
      this.colorFormat = colorFormat;
      this.shaderModules = shaderModules;
      this.vertexInputStateCreateInfo = vertexInputStateCreateInfo;
   }

   public int getDepthFormat() {
      return depthFormat;
   }

   public void setDepthFormat(int depthFormat) {
      this.depthFormat = depthFormat;
   }

   public boolean hasDepthFormat() {
      return (depthFormat != VK14.VK_FORMAT_UNDEFINED);
   }

   public List<DescriptorSetLayout> getDescriptorSetLayouts() {
      return descriptorSetLayouts;
   }

   public void setDescriptorSetLayouts(List<DescriptorSetLayout> descriptorSetLayouts) {
      this.descriptorSetLayouts = descriptorSetLayouts;
   }

   public List<PushConstantRange> getPushConstantRanges() {
      return pushConstantRanges;
   }

   public void setPushConstantRanges(List<PushConstantRange> pushConstantRanges) {
      this.pushConstantRanges = pushConstantRanges;
   }

   public int getColorFormat() {
      return colorFormat;
   }

   public List<ShaderModule> getShaderModules() {
      return shaderModules;
   }

   public VkPipelineVertexInputStateCreateInfo getVertexInputStateCreateInfo() {
      return vertexInputStateCreateInfo;
   }
}
