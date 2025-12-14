package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.MaterialCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.ModelCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.TextureCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VertexBufferStructure;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanPushConstantsHandler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanTexture;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.PushConstantRange;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class SceneRenderer {
   private static final Logger logger = LoggerFactory.getLogger(SceneRenderer.class);
   private VkClearValue clearValueColor;
   private VkClearValue clearValueDepth;
   private final Pipeline pipeline;
   private List<Attachment> attachmentDepth = new ArrayList<>();
   private List<VkRenderingAttachmentInfo.Buffer> attachmentInfoColor = new ArrayList<>();
   private List<VkRenderingAttachmentInfo> attachmentInfoDepth = new ArrayList<>();
   private List<VkRenderingInfo> renderingInfos = new ArrayList<>();
   private final VulkanBuffer projectionMatrixBuffer;
   private final DescriptorSetLayout fragmentStorageLayout;
   private final DescriptorSetLayout textureLayout;
   private final DescriptorSetLayout vertexUniformLayout;
   private final VulkanTextureSampler textureSampler;


   private static final int DEPTH_FORMAT = VK13.VK_FORMAT_D16_UNORM;
   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";
   private static final boolean DEBUG_SHADERS = true;
   private static final String DESC_ID_MATERIALS = "SCN_DESC_ID_MAT";
   private static final String DESC_ID_PROJECTION = "SCN_DESC_ID_PROJ";
   private static final String DESC_ID_TEXTURE = "SCN_DESC_ID_TEX";

   public SceneRenderer(VulkanSwapChain swapChain, VulkanSurface surface, PipelineCache cache, LogicalDevice device, DescriptorAllocator allocator, VulkanScene scene) {
      this.clearValueColor = VkClearValue.calloc().color(
              c -> c.float32(0, 0f)
                      .float32(1, 0f)
                      .float32(2, 0f)
                      .float32(3, 1f));
      this.clearValueDepth = VkClearValue.calloc().color(c -> c.float32(0, 1f));
      initDepthAttachments(swapChain, device);
      initColorAttachmentsInfo(swapChain);
      initDepthAttachmentsInfo(swapChain);
      initRenderInfos(swapChain);
      List<ShaderModule> shaderModules = createShaderModules(device);

      this.vertexUniformLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_VERTEX_BIT));
      this.projectionMatrixBuffer = VulkanUtils.createHostVisibleBuffer(device, allocator, VulkanConstants.MAT4X4_SIZE,
              VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_PROJECTION, vertexUniformLayout);
      VulkanUtils.copyMatrixToBuffer(device, projectionMatrixBuffer, scene.getProjection().getProjectionMatrix(), 0);

      this.fragmentStorageLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));

      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT,
              VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK, 1, true);
      this.textureLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
              0, TextureCache.MAX_TEXTURES, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));

      this.pipeline = createPipeline(device, shaderModules, surface, cache);
      shaderModules.forEach(s -> s.cleanup(device));
   }

   private void initColorAttachmentsInfo(VulkanSwapChain swapChain) {
      int numImages = swapChain.getNumImages();
      for(int i = 0; i < numImages; i++) {
         attachmentInfoColor.add(VkRenderingAttachmentInfo.calloc(1)
                 .sType$Default()
                 .imageView(swapChain.getImageViews().get(i).getImageViewId())
                 .imageLayout(KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                 .clearValue(clearValueColor));
      }
   }

   private void initDepthAttachments(VulkanSwapChain swapChain, LogicalDevice device) {
      VkExtent2D extent = swapChain.getSwapChainExtent();
      for(int i = 0; i < swapChain.getNumImages(); i++) {
         this.attachmentDepth.add(new Attachment(device, extent.width(), extent.height(),
                 DEPTH_FORMAT, VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT));
      }
   }

   private void initDepthAttachmentsInfo(VulkanSwapChain swapChain) {
      for(int i = 0; i < swapChain.getNumImages(); i++) {
         this.attachmentInfoDepth.add(VkRenderingAttachmentInfo.calloc()
                 .sType$Default()
                 .imageView(attachmentDepth.get(i).getImageView().getImageViewId())
                 .imageLayout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                 .clearValue(clearValueDepth));
      }
   }

   private void initRenderInfos(VulkanSwapChain swapChain) {
      assert !attachmentInfoColor.isEmpty() : "Cannot init render infos!  Are these in the correct order?";
      try(var stack = MemoryStack.stackPush()) {
         var renderArea = VkRect2D.calloc(stack).extent(swapChain.getSwapChainExtent());
         for(int i = 0; i < swapChain.getNumImages(); i++) {
            renderingInfos.add(VkRenderingInfo.calloc()
                    .sType$Default()
                    .renderArea(renderArea)
                    .layerCount(1)
                    .pColorAttachments(attachmentInfoColor.get(i))
                    .pDepthAttachment(attachmentInfoDepth.get(i)));
         }
      }
   }

   private Pipeline createPipeline(LogicalDevice device, List<ShaderModule> shaderModules, VulkanSurface surface, PipelineCache cache) {
      var vertexBufferStructure = new VertexBufferStructure();
      var info = new PipelineBuildInfo(shaderModules, vertexBufferStructure.getVertexInputStateCreateInfo(),
              surface.getSurfaceFormat().imageFormat(), DEPTH_FORMAT, List.of(
                      new PushConstantRange(VK13.VK_SHADER_STAGE_VERTEX_BIT,0, VulkanConstants.MAT4X4_SIZE),
                      new PushConstantRange(VK13.VK_SHADER_STAGE_FRAGMENT_BIT, VulkanConstants.MAT4X4_SIZE, VulkanConstants.INT_SIZE)),
              List.of(vertexUniformLayout, fragmentStorageLayout, textureLayout));
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

   public void cleanup(LogicalDevice device) {
      pipeline.cleanup(device);
      projectionMatrixBuffer.cleanup(device);
      vertexUniformLayout.cleanup(device);
      fragmentStorageLayout.cleanup(device);
      textureLayout.cleanup(device);
      textureSampler.cleanup(device);
      renderingInfos.forEach(VkRenderingInfo::free);
      attachmentInfoDepth.forEach(VkRenderingAttachmentInfo::free);
      attachmentInfoColor.forEach(VkRenderingAttachmentInfo.Buffer::free);
      attachmentDepth.forEach(a -> a.cleanup(device));
      VulkanPushConstantsHandler.free();
      clearValueDepth.free();
      clearValueColor.free();
   }

   public void render(VulkanSwapChain swapChain, CommandBuffer commandBuffer, ModelCache modelCache, int imageIndex, VulkanScene scene, DescriptorAllocator allocator) {
      try(var stack = MemoryStack.stackPush()) {
         long swapChainImage = swapChain.getImageView(imageIndex).getImageId();
         var commandHandle = commandBuffer.getCommandBuffer();
         StructureUtils.imageBarrier(stack, commandHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         StructureUtils.imageBarrier(stack, commandHandle, attachmentDepth.get(imageIndex).getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT,
                 VK13.VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT,
                 VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_DEPTH_BIT);

         VK13.vkCmdBeginRendering(commandHandle, renderingInfos.get(imageIndex));
         VK13.vkCmdBindPipeline(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         StructureUtils.setupViewportAndScissor(stack, swapChain.getSwapChainExtent().width(), swapChain.getSwapChainExtent().height(), commandHandle);

         LongBuffer descriptorSetsBuf = stack.mallocLong(3)
                         .put(0, allocator.getDescriptorSet(DESC_ID_PROJECTION).getId())
                         .put(1, allocator.getDescriptorSet(DESC_ID_MATERIALS).getId())
                         .put(2, allocator.getDescriptorSet(DESC_ID_TEXTURE).getId());
         VK13.vkCmdBindDescriptorSets(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayoutId(),
                 0, descriptorSetsBuf, null);

         scene.getEntities().forEach(e -> {
            VulkanModel model = modelCache.getModel(e.getModelId());
            model.bindMeshes(stack, commandHandle, pipeline.getLayoutId(), e.getModelMatrix());
         });

         VK13.vkCmdEndRendering(commandHandle);
         StructureUtils.imageBarrier(stack, commandHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT,
                 VK13.VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_PIPELINE_STAGE_2_NONE, VK13.VK_IMAGE_ASPECT_COLOR_BIT);
      }
   }

   public void resize(LogicalDevice device, VulkanSwapChain swapChain, VulkanScene scene) {
      renderingInfos.forEach(VkRenderingInfo::free);
      attachmentInfoDepth.forEach(VkRenderingAttachmentInfo::free);
      attachmentInfoColor.forEach(VkRenderingAttachmentInfo.Buffer::free);
      attachmentDepth.forEach(a -> a.cleanup(device));
      initDepthAttachments(swapChain, device);
      initColorAttachmentsInfo(swapChain);
      initDepthAttachmentsInfo(swapChain);
      initRenderInfos(swapChain);
      VulkanUtils.copyMatrixToBuffer(device, projectionMatrixBuffer, scene.getProjection().getProjectionMatrix(), 0);
   }

   public void loadMaterials(LogicalDevice device, DescriptorAllocator allocator, MaterialCache materialCache, TextureCache textureCache) {
      var descriptorSet = allocator.addDescriptorSet(device, DESC_ID_MATERIALS, fragmentStorageLayout);
      var layoutInfo = fragmentStorageLayout.getLayoutInfo();
      var buf = materialCache.getMaterialsBuffer();
      descriptorSet.setBuffer(device, buf, buf.getRequestedSize(), layoutInfo.binding(), layoutInfo.descriptorType());

      List<VulkanImageView> imageViews = textureCache.getAsList().stream().map(VulkanTexture::getImageView).toList();
      descriptorSet = allocator.addDescriptorSet(device, DESC_ID_TEXTURE, textureLayout);
      descriptorSet.setImagesArray(device, imageViews, textureSampler, 0);
   }
}
