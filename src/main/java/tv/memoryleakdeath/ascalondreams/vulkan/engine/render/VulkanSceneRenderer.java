package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.common.CommonUtils;
import tv.memoryleakdeath.ascalondreams.common.model.Entity;
import tv.memoryleakdeath.ascalondreams.common.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.TextureSampler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.TextureSamplerInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanTextureCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptors.LayoutInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.*;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pipeline.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pipeline.PushConstantRange;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModuleData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.VulkanShaderProgram;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VertexBufferUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.ViewportUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanSceneRenderer {
   private static final Logger logger = LoggerFactory.getLogger(VulkanSceneRenderer.class);
   private final List<Fence> fences = new ArrayList<>();
   private final List<VulkanFrameBuffer> frameBuffers = new ArrayList<>();
   //private final VulkanSwapChainRenderPass renderPass;
   private final VulkanShaderProgram shaderProgram;
   private final Pipeline pipeline;

   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/fwd_fragment.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/fwd_vertex.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";
   private static final int PUSH_CONSTANTS_SIZE = VulkanGraphicsConstants.MATRIX_4X4_SIZE + VulkanGraphicsConstants.INT_LENGTH;

   private VkClearValue clearValueColor;
   private VkClearValue clearValueDepth;
   private List<VulkanAttachment> depthAttachments;
   private List<VkRenderingAttachmentInfo.Buffer> attachmentInfoColor;
   private List<VkRenderingAttachmentInfo> attachmentInfoDepth;
   private List<VkRenderingInfo> renderingInfos;
   private ByteBuffer pushConstantsBuffer;
   private DescriptorSetLayout vertexUniformDescriptorLayout;
   private DescriptorSetLayout fragmentStorageDescriptorLayout;
   private DescriptorSetLayout textureDescriptorLayout;
   private TextureSampler textureSampler;
   private VulkanBuffer projectionMatrixBuffer;

   public VulkanSceneRenderer(VulkanSwapChain swapChain, PipelineCache pipelineCache,
                              VulkanScene scene, DescriptorAllocator allocator,
                              VulkanSurface surface) {
      this.clearValueColor = VkClearValue.calloc().color(c -> c.float32(0, 0.0f).float32(1, 0.0f).float32(2, 0.0f).float32(3, 0.0f));
      this.clearValueDepth = VkClearValue.calloc().color(c -> c.float32(0, 1.0f));
      LogicalDevice device = swapChain.getDevice();
      this.depthAttachments = createDepthAttachments(swapChain, device);
      this.attachmentInfoColor = StructureUtils.createColorAttachmentsInfo(swapChain, clearValueColor);
      this.attachmentInfoDepth = StructureUtils.createDepthAttachmentsInfo(swapChain, depthAttachments, clearValueDepth);
      this.renderingInfos = StructureUtils.createRenderingInfo(swapChain, attachmentInfoColor, attachmentInfoDepth);
      this.shaderProgram = createShaderProgram(device);
      this.pushConstantsBuffer = MemoryUtil.memAlloc(PUSH_CONSTANTS_SIZE);
      this.vertexUniformDescriptorLayout = new DescriptorSetLayout(device, List.of(new LayoutInfo(VK14.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 0, 1, VK14.VK_SHADER_STAGE_VERTEX_BIT)));
      this.projectionMatrixBuffer = VulkanUtils.createHostVisibleBuffer(device, allocator, VulkanGraphicsConstants.MATRIX_4X4_SIZE, VK14.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, "SCN_DESC_ID_PROJ", vertexUniformDescriptorLayout);
      VulkanUtils.copyMatrixToBuffer(projectionMatrixBuffer, scene.getProjection().getProjectionMatrix(), 0);
      this.fragmentStorageDescriptorLayout = new DescriptorSetLayout(device, List.of(new LayoutInfo(VK14.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 0, 1, VK14.VK_SHADER_STAGE_FRAGMENT_BIT)));
      this.textureSampler = createTextureSampler(device);
      this.textureDescriptorLayout = new DescriptorSetLayout(device, List.of(new LayoutInfo(VK14.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 0, VulkanTextureCache.MAX_TEXTURES, VK14.VK_SHADER_STAGE_FRAGMENT_BIT)));
//      this.renderPass = new VulkanSwapChainRenderPass(swapChain, depthAttachments.getFirst().getImage().getImageData().getFormat());
//      createFrameBuffers(swapChain.getWidth(), swapChain.getHeight(), device);
      this.pipeline = createPipeline(pipelineCache, surface, List.of(vertexUniformDescriptorLayout, fragmentStorageDescriptorLayout, textureDescriptorLayout));
      shaderProgram.cleanup();
//      swapChain.getImageViews().forEach(view -> {
//         VulkanCommandBuffer commandBuffer = new VulkanCommandBuffer(pool, true, false);
//         Fence fence = new Fence(device, true);
//         this.commandBuffers.add(commandBuffer);
//         this.fences.add(fence);
//      });
   }

   private VulkanShaderProgram createShaderProgram(LogicalDevice device) {
      ShaderCompiler.compileIfModified(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader);
      ShaderCompiler.compileIfModified(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader);
      return new VulkanShaderProgram(device, List.of(new ShaderModuleData(VK14.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV),
              new ShaderModuleData(VK14.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV)));

   }

   private Pipeline createPipeline(PipelineCache cache, VulkanSurface surface, List<DescriptorSetLayout> setLayouts) {
      VertexBufferUtil.Components vertexBuffer = VertexBufferUtil.createBuffer();
      PipelineBuildInfo info = new PipelineBuildInfo(surface.getSurfaceFormats().format(), shaderProgram.getShaderModules(), vertexBuffer.info());
      info.setDepthFormat(VK14.VK_FORMAT_D16_UNORM);
      info.setPushConstantRanges(List.of(new PushConstantRange(VK14.VK_SHADER_STAGE_VERTEX_BIT, 0, VulkanGraphicsConstants.MATRIX_4X4_SIZE),
              new PushConstantRange(VK14.VK_SHADER_STAGE_FRAGMENT_BIT, VulkanGraphicsConstants.MATRIX_4X4_SIZE, VulkanGraphicsConstants.INT_LENGTH)));
      info.setDescriptorSetLayouts(setLayouts);
      Pipeline pipeline = new Pipeline(cache, info);
      VertexBufferUtil.clean(vertexBuffer);
      return pipeline;
   }

   private List<VulkanAttachment> createDepthAttachments(VulkanSwapChain swapChain, LogicalDevice device) {
      int numImages = swapChain.getNumImages();
      VkExtent2D swapChainExtent = swapChain.getExtent();
      List<VulkanAttachment> depthAttachments = new ArrayList<>();
      for (int i = 0; i < numImages; i++) {
         depthAttachments.add(new VulkanAttachment(device, swapChainExtent.width(), swapChainExtent.height(),
                 VK14.VK_FORMAT_D16_UNORM, VK14.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT));
      }
      return depthAttachments;
   }

//   private void createFrameBuffers(int width, int height, LogicalDevice device) {
//      try (MemoryStack stack = MemoryStack.stackPush()) {
//         LongBuffer attachments = stack.mallocLong(2);
//         swapChain.getImageViews().forEach(CommonUtils.withIndex((index, view) -> {
//            attachments.put(0, view.getId());
//            attachments.put(1, depthAttachments.get(index).getView().getId());
//            VulkanFrameBuffer frameBuffer = new VulkanFrameBuffer(device, width, height, attachments, renderPass.getId());
//            this.frameBuffers.add(frameBuffer);
//         }));
//      }
//   }

   private TextureSampler createTextureSampler(LogicalDevice device) {
      TextureSamplerInfo info = new TextureSamplerInfo(VK14.VK_SAMPLER_ADDRESS_MODE_REPEAT, VK14.VK_BORDER_COLOR_INT_OPAQUE_BLACK, 1, true);
      return new TextureSampler(device, info);
   }

//   public void recordCommandBuffer(List<VulkanModel> models) {
//      try (MemoryStack stack = MemoryStack.stackPush()) {
//         VkExtent2D swapChainExtent = swapChain.getExtent();
//         int swapChainWidth = swapChainExtent.width();
//         int swapChainHeight = swapChainExtent.height();
//         int frame = swapChain.getCurrentFrame();
//
//         VulkanCommandBuffer commandBuffer = commandBuffers.get(frame);
//         VulkanFrameBuffer frameBuffer = frameBuffers.get(frame);
//
//         commandBuffer.reset();
//         VkRenderPassBeginInfo beginInfo = ViewportUtils.initViewport(stack, renderPass, swapChainWidth, swapChainHeight, frameBuffer);
//         commandBuffer.beginRecording();
//         VK14.vkCmdBeginRenderPass(commandBuffer.getBuffer(), beginInfo, VK14.VK_SUBPASS_CONTENTS_INLINE);
//         VK14.vkCmdBindPipeline(commandBuffer.getBuffer(), VK14.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
//
//         ViewportUtils.createViewport(stack, swapChainHeight, swapChainWidth, commandBuffer.getBuffer());
//         ViewportUtils.createScissor(stack, swapChainWidth, swapChainHeight, commandBuffer.getBuffer());
//
//         LongBuffer offsets = stack.mallocLong(1);
//         offsets.put(0, 0L);
//         LongBuffer vertexBuffer = stack.mallocLong(1);
//         ByteBuffer pushConstantsBuffer = stack.malloc(VulkanGraphicsConstants.MATRIX_4X4_SIZE * 2);
//         models.forEach(model -> {
//            List<Entity> entities = scene.getEntitiesByModelId(model.getId());
//            if (entities.isEmpty()) {
//               return;
//            }
//            model.bindMeshes(commandBuffer.getBuffer(), vertexBuffer, offsets, scene,
//                    entities, pushConstantsBuffer, pipeline.getLayoutId());
//         });
//         VK14.vkCmdEndRenderPass(commandBuffer.getBuffer());
//         commandBuffer.endRecording();
//      }
//   }

   public void resize(VulkanSwapChain swapChain) {
//      this.swapChain = swapChain;
      frameBuffers.forEach(VulkanFrameBuffer::cleanup);
      frameBuffers.clear();
      depthAttachments.forEach(VulkanAttachment::cleanup);
      depthAttachments.clear();
      createDepthAttachments(swapChain.getWidth(), swapChain.getHeight(), swapChain.getDevice());
//      createFrameBuffers(swapChain.getWidth(), swapChain.getHeight(), swapChain.getDevice());
   }

   public void submit(BaseDeviceQueue queue) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         int currentFrame = swapChain.getCurrentFrame();
         Fence currentFence = fences.get(currentFrame);
         VulkanCommandBuffer commandBuffer = commandBuffers.get(currentFrame);
         currentFence.reset();
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
      pipeline.cleanup();
      depthAttachments.forEach(VulkanAttachment::cleanup);
      depthAttachments.clear();
      shaderProgram.cleanup();
      frameBuffers.forEach(VulkanFrameBuffer::cleanup);
      frameBuffers.clear();
      renderPass.cleanup();
      commandBuffers.forEach(VulkanCommandBuffer::cleanup);
      commandBuffers.clear();
      fences.forEach(Fence::cleanup);
      fences.clear();
   }
}
