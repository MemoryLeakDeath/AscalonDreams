package tv.memoryleakdeath.ascalondreams.gui;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;

public class GuiVertexBufferStructure {
   private static final Logger logger = LoggerFactory.getLogger(GuiVertexBufferStructure.class);
   public static final int VERTEX_SIZE = VulkanConstants.FLOAT_SIZE * 5;
   private static final int NUMBER_OF_ATTRIBUTES = 3;

   private final VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo;
   private final VkVertexInputAttributeDescription.Buffer vertexInputAttributes;
   private final VkVertexInputBindingDescription.Buffer vertexInputBindings;

   public GuiVertexBufferStructure() {
      this.vertexInputAttributes = VkVertexInputAttributeDescription.calloc(NUMBER_OF_ATTRIBUTES);
      this.vertexInputBindings = VkVertexInputBindingDescription.calloc(1);
      this.vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc();

      int offset = 0;
      // Position
      vertexInputAttributes.get(0)
              .binding(0)
              .location(0)
              .format(VK13.VK_FORMAT_R32G32_SFLOAT)
              .offset(offset);
      // Texture coords
      offset += VulkanConstants.FLOAT_SIZE * 2;
      vertexInputAttributes.get(1)
              .binding(0)
              .location(1)
              .format(VK13.VK_FORMAT_R32G32_SFLOAT)
              .offset(offset);
      // color
      offset += VulkanConstants.FLOAT_SIZE * 2;
      vertexInputAttributes.get(2)
              .binding(0)
              .location(2)
              .format(VK13.VK_FORMAT_R8G8B8A8_UNORM)
              .offset(offset);

      vertexInputBindings.get(0)
              .binding(0)
              .stride(VERTEX_SIZE)
              .inputRate(VK13.VK_VERTEX_INPUT_RATE_VERTEX);

      vertexInputStateCreateInfo.sType$Default().pVertexBindingDescriptions(vertexInputBindings)
              .pVertexAttributeDescriptions(vertexInputAttributes);
   }

   public void cleanup() {
      vertexInputBindings.free();
      vertexInputAttributes.free();
      vertexInputStateCreateInfo.free();
   }

   public VkPipelineVertexInputStateCreateInfo getVertexInputStateCreateInfo() {
      return vertexInputStateCreateInfo;
   }
}
