package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.ModelCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VertexBufferStructure;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.util.ArrayList;
import java.util.List;

public class SceneRenderer {
   private static final Logger logger = LoggerFactory.getLogger(SceneRenderer.class);
   private VkClearValue clearValue;
   private List<VkRenderingAttachmentInfo.Buffer> colorAttachmentInfo = new ArrayList<>();
   private List<VkRenderingInfo> renderingInfos = new ArrayList<>();
   private final Pipeline pipeline;

   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";
   private static final boolean DEBUG_SHADERS = true;

   public SceneRenderer(VulkanSwapChain swapChain, VulkanSurface surface, PipelineCache cache, LogicalDevice device) {
      this.clearValue = VkClearValue.calloc().color(
              c -> c.float32(0, 0f)
                      .float32(1, 0f)
                      .float32(2, 0f)
                      .float32(3, 1f));
      initColorAttachmentsInfo(swapChain);
      initRenderInfos(swapChain);
      List<ShaderModule> shaderModules = createShaderModules(device);
      this.pipeline = createPipeline(device, shaderModules, surface, cache);
      shaderModules.forEach(s -> s.cleanup(device));
   }

   private void initColorAttachmentsInfo(VulkanSwapChain swapChain) {
      int numImages = swapChain.getNumImages();
      for(int i = 0; i < numImages; i++) {
         colorAttachmentInfo.add(VkRenderingAttachmentInfo.calloc(1)
                 .sType$Default()
                 .imageView(swapChain.getImageViews().get(i).getImageViewId())
                 .imageLayout(KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                 .clearValue(clearValue));
      }
   }

   private void initRenderInfos(VulkanSwapChain swapChain) {
      assert !colorAttachmentInfo.isEmpty() : "Cannot init render infos!  Are these in the correct order?";
      try(var stack = MemoryStack.stackPush()) {
         var renderArea = VkRect2D.calloc(stack).extent(swapChain.getSwapChainExtent());
         renderingInfos = colorAttachmentInfo.stream().map(i -> VkRenderingInfo.calloc()
                 .sType$Default()
                 .renderArea(renderArea)
                 .layerCount(1)
                 .pColorAttachments(i)).toList();
      }
   }

   private static Pipeline createPipeline(LogicalDevice device, List<ShaderModule> shaderModules, VulkanSurface surface, PipelineCache cache) {
      var vertexBufferStructure = new VertexBufferStructure();
      var info = new PipelineBuildInfo(shaderModules, vertexBufferStructure.getVertexInputStateCreateInfo(), surface.getSurfaceFormat().imageFormat());
      var pipeline = new Pipeline(device, cache, info);
      vertexBufferStructure.cleanup();
      return pipeline;
   }

   private static List<ShaderModule> createShaderModules(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, DEBUG_SHADERS);
      ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, DEBUG_SHADERS);
      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV));
   }

   public void cleanup() {
      renderingInfos.forEach(VkRenderingInfo::free);
      colorAttachmentInfo.forEach(VkRenderingAttachmentInfo.Buffer::free);
      clearValue.free();
   }

   public void render(VulkanSwapChain swapChain, CommandBuffer commandBuffer, ModelCache modelCache, int imageIndex) {
      try(var stack = MemoryStack.stackPush()) {
         long swapChainImage = swapChain.getImageView(imageIndex).getImageId();
         var commandHandle = commandBuffer.getCommandBuffer();
         StructureUtils.imageBarrier(stack, commandHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         VK13.vkCmdBeginRendering(commandHandle, renderingInfos.get(imageIndex));
         VK13.vkCmdBindPipeline(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         StructureUtils.setupViewportAndScissor(stack, swapChain.getSwapChainExtent().width(), swapChain.getSwapChainExtent().height(), commandHandle);
         modelCache.getModelMap().values().forEach(model -> model.bindMeshes(stack, commandHandle));

         VK13.vkCmdEndRendering(commandHandle);
         StructureUtils.imageBarrier(stack, commandHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT,
                 VK13.VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_PIPELINE_STAGE_2_NONE, VK13.VK_IMAGE_ASPECT_COLOR_BIT);
      }
   }
}
