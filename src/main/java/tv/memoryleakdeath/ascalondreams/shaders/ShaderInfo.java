package tv.memoryleakdeath.ascalondreams.shaders;

import org.lwjgl.vulkan.VkSpecializationInfo;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;

public record ShaderInfo(String file, int shaderType, int shaderStage, boolean debug) {
   public String getSpvName() {
      return file + ".spv";
   }

   public ShaderModule getShaderModule(LogicalDevice device, VkSpecializationInfo info) {
      return new ShaderModule(device, shaderStage, getSpvName(), info);
   }
}
