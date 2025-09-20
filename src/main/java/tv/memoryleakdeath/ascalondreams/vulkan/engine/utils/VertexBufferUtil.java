package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsConstants;

public class VertexBufferUtil {
   private static final int TEXTURE_COORD_COMPONENTS = 2;
   private static final int NUMBER_OF_ATTRIBUTES = 2;
   private static final int POSITION_COMPONENTS = 3;

   private VertexBufferUtil() {}

   public static Components createBuffer() {
      VkVertexInputAttributeDescription.Buffer attributes = VkVertexInputAttributeDescription.calloc(NUMBER_OF_ATTRIBUTES);
      VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.calloc(1);
      VkPipelineVertexInputStateCreateInfo info = VkPipelineVertexInputStateCreateInfo.calloc();

      // position
      attributes.get(0)
              .binding(0)
              .location(0)
              .format(VK14.VK_FORMAT_R32G32B32_SFLOAT)
              .offset(0);

      // texture coords
      int offset = POSITION_COMPONENTS * VulkanGraphicsConstants.FLOAT_LENGTH;
      attributes.get(1)
              .binding(0)
              .location(1)
              .format(VK14.VK_FORMAT_R32G32_SFLOAT)
              .offset(offset);
      int stride = offset + TEXTURE_COORD_COMPONENTS * VulkanGraphicsConstants.FLOAT_LENGTH;
      bindings.get(0)
              .binding(0)
              .stride(stride)
              .inputRate(VK14.VK_VERTEX_INPUT_RATE_VERTEX);

      info.sType$Default()
              .pVertexBindingDescriptions(bindings)
              .pVertexAttributeDescriptions(attributes);
      return new Components(attributes, bindings, info);
   }

   public static void clean(Components components) {
      components.attributes.free();
      components.bindings.free();
      components.info.free();
   }

   public record Components(VkVertexInputAttributeDescription.Buffer attributes, VkVertexInputBindingDescription.Buffer bindings, VkPipelineVertexInputStateCreateInfo info){
   }
}
