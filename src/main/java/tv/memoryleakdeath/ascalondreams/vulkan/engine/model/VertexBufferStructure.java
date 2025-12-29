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
   private static final int TEXTURE_COORD_COMPONENTS = 2;
   private static final int NORMAL_COMPONENTS = 3;
   private static final int NUM_ATTRIBUTES = 5;
   private static final int POSITION_COMPONENTS = 3;

   private final VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo;
   private final VkVertexInputAttributeDescription.Buffer vertexInputAttributes;
   private final VkVertexInputBindingDescription.Buffer vertexInputBindings;

   public VertexBufferStructure() {
      this.vertexInputAttributes = VkVertexInputAttributeDescription.calloc(NUM_ATTRIBUTES);
      this.vertexInputBindings = VkVertexInputBindingDescription.calloc(1);
      this.vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc();

      // position
      int offset = 0;
      vertexInputAttributes.get(0)
              .binding(0)
              .location(0)
              .format(VK13.VK_FORMAT_R32G32B32_SFLOAT)
              .offset(offset);
      // normal
      offset += POSITION_COMPONENTS * VulkanConstants.FLOAT_SIZE;
      vertexInputAttributes.get(1)
              .binding(0)
              .location(1)
              .format(VK13.VK_FORMAT_R32G32B32_SFLOAT)
              .offset(offset);
      // tangent
      offset += NORMAL_COMPONENTS * VulkanConstants.FLOAT_SIZE;
      vertexInputAttributes.get(2)
              .binding(0)
              .location(2)
              .format(VK13.VK_FORMAT_R32G32B32_SFLOAT)
              .offset(offset);
      // bitangent
      offset += NORMAL_COMPONENTS * VulkanConstants.FLOAT_SIZE;
      vertexInputAttributes.get(3)
              .binding(0)
              .location(3)
              .format(VK13.VK_FORMAT_R32G32B32_SFLOAT)
              .offset(offset);
      // texture coords
      offset += NORMAL_COMPONENTS * VulkanConstants.FLOAT_SIZE;
      vertexInputAttributes.get(4)
              .binding(0)
              .location(4)
              .format(VK13.VK_FORMAT_R32G32_SFLOAT)
              .offset(offset);

      int stride = offset + TEXTURE_COORD_COMPONENTS * VulkanConstants.FLOAT_SIZE;
      vertexInputBindings.get(0)
              .binding(0)
              .stride(stride)
              .inputRate(VK13.VK_VERTEX_INPUT_RATE_VERTEX);

      vertexInputStateCreateInfo
              .sType$Default()
              .pVertexBindingDescriptions(vertexInputBindings)
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
