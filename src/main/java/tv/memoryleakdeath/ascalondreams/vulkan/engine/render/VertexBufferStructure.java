package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsConstants;

public class VertexBufferStructure extends BaseVertexInputStateInfo {
   private static final int NUMBER_OF_ATTRIBUTES = 1;
   private static final int POSITION_COMPONENTS = 3;

   private final VkVertexInputAttributeDescription.Buffer attributeDescriptions;
   private final VkVertexInputBindingDescription.Buffer bindingDescriptions;

   public VertexBufferStructure() {
      this.attributeDescriptions = VkVertexInputAttributeDescription.calloc(NUMBER_OF_ATTRIBUTES);
      this.bindingDescriptions = VkVertexInputBindingDescription.calloc(1);
      setInfo(VkPipelineVertexInputStateCreateInfo.calloc());

      int i = 0;
      // position
      attributeDescriptions.get(i)
              .binding(0)
              .location(i)
              .format(VK14.VK_FORMAT_R32G32B32_SFLOAT)
              .offset(0);

      bindingDescriptions.get(0)
              .binding(0)
              .stride(POSITION_COMPONENTS * VulkanGraphicsConstants.FLOAT_LENGTH)
              .inputRate(VK14.VK_VERTEX_INPUT_RATE_VERTEX);

      getInfo().sType(VK14.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
              .pVertexBindingDescriptions(bindingDescriptions)
              .pVertexAttributeDescriptions(attributeDescriptions);
   }

   @Override
   public void cleanup() {
      super.cleanup();
      bindingDescriptions.free();
      attributeDescriptions.free();
   }
}
