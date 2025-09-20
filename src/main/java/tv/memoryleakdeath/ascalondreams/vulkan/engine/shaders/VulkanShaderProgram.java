package tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class VulkanShaderProgram {
   private LogicalDevice device;
   private List<ShaderModule> shaderModules = new ArrayList<>();

   public VulkanShaderProgram(LogicalDevice device, List<ShaderModuleData> data) {
      this.device = device;
      data.forEach(moduleData -> {
         try {
            byte[] contents = Files.readAllBytes(new File(moduleData.shaderSpvFile()).toPath());
            long moduleHandle = createModule(contents);
            shaderModules.add(new ShaderModule(moduleData.shaderStage(), moduleHandle));
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      });
   }

   public void cleanup() {
      shaderModules.forEach(module -> VK14.vkDestroyShaderModule(device.getDevice(), module.handle(), null));
   }

   private long createModule(byte[] code) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         ByteBuffer codePointer = stack.malloc(code.length).put(0, code);
         VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                 .sType$Default()
                 .pCode(codePointer);
         LongBuffer lb = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateShaderModule(device.getDevice(), createInfo, null, lb), "Failed to create shader module!");
         return lb.get(0);
      }
   }

   public List<ShaderModule> getShaderModules() {
      return shaderModules;
   }
}
