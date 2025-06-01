package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import tv.memoryleakdeath.ascalondreams.common.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Pipeline;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PipelineCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PipelineCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModuleData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.VulkanShaderProgram;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.ViewportUtils;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class ForwardRenderer {
   private final List<VulkanCommandBuffer> commandBuffers = new ArrayList<>();
   private final List<Fence> fences = new ArrayList<>();
   private final List<VulkanFrameBuffer> frameBuffers = new ArrayList<>();
   private final VulkanSwapChainRenderPass renderPass;
   private final VulkanSwapChain swapChain;
   private final VulkanShaderProgram shaderProgram;
   private final Pipeline pipeline;

   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/fwd_fragment.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/fwd_vertex.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";

   public ForwardRenderer(VulkanSwapChain swapChain, VulkanCommandPool pool, PipelineCache pipelineCache) {
      this.swapChain = swapChain;
      this.renderPass = new VulkanSwapChainRenderPass(swapChain);
      try (MemoryStack stack = MemoryStack.stackPush()) {
         LogicalDevice device = swapChain.getDevice();
         int width = swapChain.getWidth();
         int height = swapChain.getHeight();
         LongBuffer attachments = stack.mallocLong(1);

         ShaderCompiler.compileIfModified(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader);
         ShaderCompiler.compileIfModified(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader);
         this.shaderProgram = new VulkanShaderProgram(device, List.of(new ShaderModuleData(VK14.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV),
                 new ShaderModuleData(VK14.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV)));
         PipelineCreateInfo piplineInfo = new PipelineCreateInfo(renderPass.getId(), shaderProgram, 1, new VertexBufferStructure());
         this.pipeline = new Pipeline(pipelineCache, piplineInfo);
         piplineInfo.cleanup();

         swapChain.getImageViews().forEach(view -> {
            attachments.put(0, view.getId());
            VulkanFrameBuffer frameBuffer = new VulkanFrameBuffer(device, width, height, attachments, renderPass.getId());
            VulkanCommandBuffer commandBuffer = new VulkanCommandBuffer(pool, true, false);
            Fence fence = new Fence(device, true);
            this.frameBuffers.add(frameBuffer);
            this.commandBuffers.add(commandBuffer);
            this.fences.add(fence);
         });

      }
   }

   public void recordCommandBuffer(List<VulkanModel> models) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkExtent2D swapChainExtent = swapChain.getExtent();
         int swapChainWidth = swapChainExtent.width();
         int swapChainHeight = swapChainExtent.height();
         int frame = swapChain.getCurrentFrame();

         VulkanCommandBuffer commandBuffer = commandBuffers.get(frame);
         VulkanFrameBuffer frameBuffer = frameBuffers.get(frame);

         commandBuffer.reset();
         VkRenderPassBeginInfo beginInfo = ViewportUtils.initViewport(stack, renderPass, swapChainWidth, swapChainHeight, frameBuffer);
         commandBuffer.beginRecording();
         VK14.vkCmdBeginRenderPass(commandBuffer.getBuffer(), beginInfo, VK14.VK_SUBPASS_CONTENTS_INLINE);
         VK14.vkCmdBindPipeline(commandBuffer.getBuffer(), VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());

         ViewportUtils.createViewport(stack, swapChainHeight, swapChainWidth, commandBuffer.getBuffer());
         ViewportUtils.createScissor(stack, swapChainWidth, swapChainHeight, commandBuffer.getBuffer());

         LongBuffer offsets = stack.mallocLong(1);
         offsets.put(0, 0L);
         LongBuffer vertexBuffer = stack.mallocLong(1);
         models.forEach(model -> model.bindMeshes(commandBuffer.getBuffer(), vertexBuffer, offsets));
         VK14.vkCmdEndRenderPass(commandBuffer.getBuffer());
         commandBuffer.endRecording();
      }
   }

   public void submit(BaseDeviceQueue queue) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         int currentFrame = swapChain.getCurrentFrame();
         Fence currentFence = fences.get(currentFrame);
         currentFence.reset();
         VulkanCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
         VulkanSwapChain.SyncSemaphores syncSemaphores = swapChain.getSemaphoreList().get(currentFrame);
         queue.submit(stack.pointers(commandBuffer.getBuffer()),
                 stack.longs(syncSemaphores.imageAquisitionSemaphore().getId()),
                 stack.ints(VK14.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                 stack.longs(syncSemaphores.renderCompleteSemaphore().getId()),
                 currentFence);
      }
   }

   public void waitForFence() {
      fences.get(swapChain.getCurrentFrame()).waitForFence();
   }

   public void cleanup() {
      frameBuffers.forEach(VulkanFrameBuffer::cleanup);
      renderPass.cleanup();
      commandBuffers.forEach(VulkanCommandBuffer::cleanup);
      fences.forEach(Fence::cleanup);
   }
}
