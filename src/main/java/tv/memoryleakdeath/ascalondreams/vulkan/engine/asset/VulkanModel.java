package tv.memoryleakdeath.ascalondreams.vulkan.engine.asset;

import org.lwjgl.system.MemoryStack;
import tv.memoryleakdeath.ascalondreams.common.model.Model;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanCommandPool;

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
      meshList.forEach(VulkanMesh::cleanupSrcBuffers);
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
