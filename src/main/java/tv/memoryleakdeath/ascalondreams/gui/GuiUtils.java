package tv.memoryleakdeath.ascalondreams.gui;

import imgui.ImGui;
import imgui.ImVec4;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.render.Pipeline;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Map;

public class GuiUtils {
   private GuiUtils() {
   }

   public static void recordGuiRendering(MemoryStack stack, Pipeline pipeline, DescriptorAllocator allocator, VkCommandBuffer commandHandle, String descriptorSetId, Map<Long, Long> textureMap) {
      var io = ImGui.getIO();
      FloatBuffer pushConstantBuffer = stack.mallocFloat(2);
      pushConstantBuffer.put(0, 2f / io.getDisplaySizeX());
      pushConstantBuffer.put(1, -2f / io.getDisplaySizeY());
      VK13.vkCmdPushConstants(commandHandle, pipeline.getLayoutId(), VK13.VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantBuffer);

      LongBuffer descriptorSets = stack.mallocLong(1);
      var imVec4 = new ImVec4();
      VkRect2D.Buffer rect = VkRect2D.calloc(1, stack);
      var imDrawData = ImGui.getDrawData();
      int numCommandLists = imDrawData.getCmdListsCount();
      int offsetIndex = 0;
      int offsetVertex = 0;
      for(int i = 0; i < numCommandLists; i++) {
         int commandBufferSize = imDrawData.getCmdListCmdBufferSize(i);
         for(int j = 0; j < commandBufferSize; j++) {
            long textureDescriptorSet;
            long textureId = imDrawData.getCmdListCmdBufferTextureId(i, j);
            if(textureId == 0) {
               textureDescriptorSet = allocator.getDescriptorSet(descriptorSetId).getId();
            } else {
               textureDescriptorSet = textureMap.get(textureId);
            }
            descriptorSets.put(0, textureDescriptorSet);
            VK13.vkCmdBindDescriptorSets(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipeline.getLayoutId(), 0, descriptorSets, null);

            imDrawData.getCmdListCmdBufferClipRect(imVec4, i, j);
            rect.offset(it -> it.x((int) Math.max(imVec4.x, 0)).y((int) Math.max(imVec4.y, 1)));
            rect.extent(it -> it.width((int) (imVec4.z - imVec4.x)).height((int) (imVec4.w - imVec4.y)));
            VK13.vkCmdSetScissor(commandHandle, 0, rect);
            int numElements = imDrawData.getCmdListCmdBufferElemCount(i, j);
            VK13.vkCmdDrawIndexed(commandHandle, numElements, 1,
                    offsetIndex + imDrawData.getCmdListCmdBufferIdxOffset(i, j),
                    offsetVertex + imDrawData.getCmdListCmdBufferVtxOffset(i, j),
                    0);
         }
         offsetIndex += imDrawData.getCmdListCmdBufferSize(i);
         offsetVertex += imDrawData.getCmdListVtxBufferSize(i);
      }
   }
}
