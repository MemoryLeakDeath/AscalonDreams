package tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSpecializationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShaderModule {
   private static final Logger logger = LoggerFactory.getLogger(ShaderModule.class);
   private final long id;
   private final int stage;
   private final VkSpecializationInfo specializationInfo;

   public ShaderModule(LogicalDevice device, int shaderStage, String spvFile, VkSpecializationInfo info) {
      try {
         byte[] contents = Files.readAllBytes(Path.of(spvFile));
         this.id = createShaderModule(device, contents);
         this.stage = shaderStage;
         this.specializationInfo = info;
      } catch (IOException e) {
         logger.error("Error reading shader file: %s".formatted(spvFile), e);
         throw new RuntimeException(e);
      }
   }

   private long createShaderModule(LogicalDevice device, byte[] shaderCode) {
      try (var stack = MemoryStack.stackPush()) {
         ByteBuffer codeBuf = stack.malloc(shaderCode.length).put(0, shaderCode);
         var info = VkShaderModuleCreateInfo.calloc(stack)
                 .sType$Default()
                 .pCode(codeBuf);
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateShaderModule(device.getDevice(), info, null, buf), "Failed to create shader module!");
         return buf.get(0);
      }
   }

   public void cleanup(LogicalDevice device) {
      VK13.vkDestroyShaderModule(device.getDevice(), id, null);
   }

   public long getId() {
      return id;
   }

   public int getStage() {
      return stage;
   }

   public VkSpecializationInfo getSpecializationInfo() {
      return specializationInfo;
   }
}
