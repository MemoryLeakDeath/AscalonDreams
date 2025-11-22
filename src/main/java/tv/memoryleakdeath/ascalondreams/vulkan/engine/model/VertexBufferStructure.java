package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;

public class VertexBufferStructure {
   private static final Logger logger = LoggerFactory.getLogger(VertexBufferStructure.class);
   private static final int NUM_ATTRIBUTES = 1;
   private static final int POSITION_COMPONENTS = 3;

   private final VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo;
   private final VkVertexInputAttributeDescription.Buffer vertexInputAttributes;
   private final VkVertexInputBindingDescription.Buffer vertexInputBindings;

   public VertexBufferStructure() {
      this.vertexInputAttributes = VkVertexInputAttributeDescription.calloc(NUM_ATTRIBUTES);
      this.vertexInputBindings = VkVertexInputBindingDescription.calloc(1);
      this.vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc();

      vertexInputAttributes.get(0)
              .binding(0)
              .location(0)
              .format(VK13.VK_FORMAT_R32G32B32_SFLOAT)
              .offset(0);

      vertexInputBindings.get(0)
              .binding(0)
              .stride(POSITION_COMPONENTS * VulkanConstants.FLOAT_SIZE)
              .inputRate(VK13.VK_VERTEX_INPUT_RATE_VERTEX);

      vertexInputStateCreateInfo
              .sType$Default()
              .pVertexBindingDescriptions(vertexInputBindings)
              .pVertexAttributeDescriptions(vertexInputAttributes);
   }

   public void cleanup() {
      vertexInputBindings.free();
      vertexInputAttributes.free();
   }

   public VkPipelineVertexInputStateCreateInfo getVertexInputStateCreateInfo() {
      return vertexInputStateCreateInfo;
   }
}
