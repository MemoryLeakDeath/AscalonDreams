package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkCommandBuffer;
import tv.memoryleakdeath.ascalondreams.common.model.Model;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandPool;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanModel {
   private String id;
   private Model model;
   private List<VulkanMesh> meshList = new ArrayList<>();

   public VulkanModel(String id, Model model) {
      this.id = id;
      this.model = model;
   }

   public void prepareModel(VulkanCommandPool commandPool, BaseDeviceQueue queue) {
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
   }

   public void bindMeshes(VkCommandBuffer cmd, LongBuffer vertexBuffer, LongBuffer offsets) {
      meshList.forEach(mesh -> {
         vertexBuffer.put(0, mesh.getVertexBuffer().getId());
         VK14.vkCmdBindVertexBuffers(cmd, 0, vertexBuffer, offsets);
         VK14.vkCmdBindIndexBuffer(cmd, mesh.getIndexBuffer().getId(), 0, VK14.VK_INDEX_TYPE_UINT32);
         VK14.vkCmdDrawIndexed(cmd, mesh.getMesh().getNumVertices(), 1, 0, 0, 0);
      });
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
      meshList.forEach(VulkanMesh::cleanup);
   }
}
