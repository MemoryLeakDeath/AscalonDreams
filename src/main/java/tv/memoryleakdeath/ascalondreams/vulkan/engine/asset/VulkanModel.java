package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.common.model.Entity;
import tv.memoryleakdeath.ascalondreams.common.model.Model;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanGraphicsConstants;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanModel {
   private static final Logger logger = LoggerFactory.getLogger(VulkanModel.class);
   private String id;
   private Model model;
   private List<VulkanMesh> meshList = new ArrayList<>();

   public VulkanModel(String id, Model model) {
      this.id = id;
      this.model = model;
   }

   public void prepareModel(VulkanCommandPool commandPool, BaseDeviceQueue queue) {
      logger.debug("starting prepareModel for model id: {}", id);
      LogicalDevice device = commandPool.getDevice();
      VulkanCommandBuffer commandBuffer = new VulkanCommandBuffer(commandPool, true, true);
      commandBuffer.beginRecording();
      model.getMeshes().forEach(mesh -> {
         VulkanMesh vulMesh = new VulkanMesh(mesh);
         vulMesh.prepareMesh(device, commandBuffer);
         meshList.add(vulMesh);
      });
      commandBuffer.endRecording();
      Fence fence = new Fence(device, true);
      fence.reset();
      try (MemoryStack stack = MemoryStack.stackPush()) {
         queue.submit(stack.pointers(commandBuffer.getBuffer()), null, null, null, fence);
      }
      fence.waitForFence();
      fence.cleanup();
      commandBuffer.cleanup();
      meshList.forEach(VulkanMesh::cleanSrcTransferBuffers);
      logger.debug("model id: {} prepared!", id);
   }

   public void bindMeshes(VkCommandBuffer cmd, LongBuffer vertexBuffer, LongBuffer offsets, VulkanScene scene, List<Entity> entities, ByteBuffer pushConstantsBuffer, long pipelineLayout) {
      logger.debug("binding meshes for model id: {} number of meshes: {}", id, meshList.size());
      meshList.forEach(mesh -> {
         vertexBuffer.put(0, mesh.getVertexBuffer().getId());
         VK14.vkCmdBindVertexBuffers(cmd, 0, vertexBuffer, offsets);
         VK14.vkCmdBindIndexBuffer(cmd, mesh.getIndexBuffer().getId(), 0, VK14.VK_INDEX_TYPE_UINT32);
         entities.forEach(e -> {
            setPushConstants(cmd, scene.getProjection().getProjectionMatrix(), e.getModelMatrix(), pushConstantsBuffer, pipelineLayout);
            VK14.vkCmdDrawIndexed(cmd, mesh.getMesh().getIndexes().length, 1, 0, 0, 0);
         });
      });
   }

   private void setPushConstants(VkCommandBuffer cmd, Matrix4f projectionMatrix, Matrix4f modelMatrix, ByteBuffer pushConstantsBuffer, long pipelineLayout) {
      projectionMatrix.get(pushConstantsBuffer);
      modelMatrix.get(VulkanGraphicsConstants.MATRIX_4X4_SIZE, pushConstantsBuffer);
      VK14.vkCmdPushConstants(cmd, pipelineLayout, VK14.VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantsBuffer);
   }

   public String getId() {
      return id;
   }

   public Model getModel() {
      return model;
   }

   public List<VulkanMesh> getMeshList() {
      return meshList;
   }

   public void cleanup() {
      logger.debug("Cleaning up mesh id: {}", id);
      meshList.forEach(VulkanMesh::cleanup);
   }
}
