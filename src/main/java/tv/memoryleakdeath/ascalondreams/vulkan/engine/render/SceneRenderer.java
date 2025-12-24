package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
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
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;
import java.util.List;

public class SceneRenderer {
   private static final Logger logger = LoggerFactory.getLogger(SceneRenderer.class);
   private VkClearValue clearValueColor;
   private VkClearValue clearValueDepth;
   private final Pipeline pipeline;
   private Attachment attachmentColor;
   private Attachment attachmentDepth;
   private VkRenderingAttachmentInfo.Buffer attachmentInfoColor;
   private VkRenderingAttachmentInfo attachmentInfoDepth;
   private VkRenderingInfo renderingInfo;
   private final VulkanBuffer projectionMatrixBuffer;
   private final List<VulkanBuffer> viewMatrixBuffers;
   private final DescriptorSetLayout fragmentStorageLayout;
   private final DescriptorSetLayout textureLayout;
   private final DescriptorSetLayout vertexUniformLayout;
   private final VulkanTextureSampler textureSampler;


   private static final int COLOR_FORMAT = VK13.VK_FORMAT_R16G16B16A16_SFLOAT;
   private static final int DEPTH_FORMAT = VK13.VK_FORMAT_D16_UNORM;
   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";
   private static final boolean DEBUG_SHADERS = true;
   private static final String DESC_ID_MATERIALS = "SCN_DESC_ID_MAT";
   private static final String DESC_ID_PROJECTION = "SCN_DESC_ID_PROJ";
   private static final String DESC_ID_TEXTURE = "SCN_DESC_ID_TEX";
   private static final String DESC_ID_VIEW = "SCN_DESC_ID_VIEW";

   public SceneRenderer(VulkanSwapChain swapChain, PipelineCache cache, LogicalDevice device, DescriptorAllocator allocator, VulkanScene scene, MemoryAllocationUtil allocationUtil) {
      this.clearValueColor = VkClearValue.calloc().color(
              c -> c.float32(0, 0f)
                      .float32(1, 0.0f)
                      .float32(2, 0.0f)
                      .float32(3, 1f));
      this.clearValueDepth = VkClearValue.calloc().color(c -> c.float32(0, 1f));
      this.attachmentColor = initColorAttachment(device, allocationUtil, swapChain);
      this.attachmentDepth = initDepthAttachment(swapChain, device, allocationUtil);
      this.attachmentInfoColor = initColorAttachmentInfo(attachmentColor, clearValueColor);
      this.attachmentInfoDepth = initDepthAttachmentInfo(attachmentDepth, clearValueDepth);
      this.renderingInfo = initRenderInfo(attachmentColor, attachmentInfoColor, attachmentInfoDepth);

      List<ShaderModule> shaderModules = createShaderModules(device);
      // push constants moved to PushConstantsHandler

      this.vertexUniformLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_VERTEX_BIT));
      this.projectionMatrixBuffer = VulkanUtils.createHostVisibleBuffer(device, allocationUtil, allocator, VulkanConstants.MAT4X4_SIZE,
              VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_PROJECTION, vertexUniformLayout);
      VulkanUtils.copyMatrixToBuffer(device, allocationUtil, projectionMatrixBuffer, scene.getProjection().getProjectionMatrix(), 0);

      this.viewMatrixBuffers = VulkanUtils.createHostVisibleBuffers(device, allocationUtil, allocator, VulkanConstants.MAT4X4_SIZE,
              VulkanConstants.MAX_IN_FLIGHT, VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_VIEW, vertexUniformLayout);

      this.fragmentStorageLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));

      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT,
              VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK, 1, true);
      this.textureLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
              0, TextureCache.MAX_TEXTURES, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));

      this.pipeline = createPipeline(device, shaderModules, cache);
      shaderModules.forEach(s -> s.cleanup(device));
   }

   private static Attachment initColorAttachment(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanSwapChain swapChain) {
      return new Attachment(device, allocationUtil, swapChain.getSwapChainExtent().width(),
              swapChain.getSwapChainExtent().height(), COLOR_FORMAT, VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
   }

   private static VkRenderingAttachmentInfo.Buffer initColorAttachmentInfo(Attachment attachment, VkClearValue clearValue) {
      return VkRenderingAttachmentInfo.calloc(1)
              .sType$Default()
              .imageView(attachment.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
              .clearValue(clearValue);
   }

   private static Attachment initDepthAttachment(VulkanSwapChain swapChain, LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      VkExtent2D extent = swapChain.getSwapChainExtent();
      return new Attachment(device, allocationUtil, extent.width(), extent.height(), DEPTH_FORMAT, VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
   }

   private static VkRenderingAttachmentInfo initDepthAttachmentInfo(Attachment attachmentDepth, VkClearValue clearValue) {
      return VkRenderingAttachmentInfo.calloc()
              .sType$Default()
              .imageView(attachmentDepth.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
              .clearValue(clearValue);
   }

   private static VkRenderingInfo initRenderInfo(Attachment colorAttachment, VkRenderingAttachmentInfo.Buffer colorInfo,
                               VkRenderingAttachmentInfo depthInfo) {
      try(var stack = MemoryStack.stackPush()) {
         var extent = VkExtent2D.calloc(stack).width(colorAttachment.getImage().getWidth())
                 .height(colorAttachment.getImage().getHeight());
         var renderArea = VkRect2D.calloc(stack).extent(extent);
         return VkRenderingInfo.calloc()
                 .sType$Default()
                 .renderArea(renderArea)
                 .layerCount(1)
                 .pColorAttachments(colorInfo)
                 .pDepthAttachment(depthInfo);
      }
   }

   private Pipeline createPipeline(LogicalDevice device, List<ShaderModule> shaderModules, PipelineCache cache) {
      var vertexBufferStructure = new VertexBufferStructure();
      var info = new PipelineBuildInfo(shaderModules, vertexBufferStructure.getVertexInputStateCreateInfo(),
              COLOR_FORMAT, DEPTH_FORMAT, List.of(
                      new PushConstantRange(VK13.VK_SHADER_STAGE_VERTEX_BIT,0, VulkanConstants.MAT4X4_SIZE),
                      new PushConstantRange(VK13.VK_SHADER_STAGE_FRAGMENT_BIT, VulkanConstants.MAT4X4_SIZE, VulkanConstants.INT_SIZE)),
              List.of(vertexUniformLayout, vertexUniformLayout, fragmentStorageLayout, textureLayout), true);
      var pipeline = new Pipeline(device, cache, info);
      vertexBufferStructure.cleanup();
      return pipeline;
   }

   private static List<ShaderModule> createShaderModules(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, DEBUG_SHADERS);
      ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, DEBUG_SHADERS);
      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV, null));
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      pipeline.cleanup(device);
      viewMatrixBuffers.forEach(b -> b.cleanup(device, allocationUtil));
      projectionMatrixBuffer.cleanup(device, allocationUtil);
      vertexUniformLayout.cleanup(device);
      fragmentStorageLayout.cleanup(device);
      textureLayout.cleanup(device);
      textureSampler.cleanup(device);
      renderingInfo.free();
      attachmentInfoDepth.free();
      attachmentInfoColor.free();
      attachmentColor.cleanup(device, allocationUtil);
      attachmentDepth.cleanup(device, allocationUtil);
      VulkanPushConstantsHandler.free();
      clearValueDepth.free();
      clearValueColor.free();
   }

   public void render(CommandBuffer commandBuffer, VulkanScene scene, DescriptorAllocator allocator,
                      int currentFrame, LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      try(var stack = MemoryStack.stackPush()) {
         var commandHandle = commandBuffer.getCommandBuffer();
         StructureUtils.imageBarrier(stack, commandHandle, attachmentColor.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         StructureUtils.imageBarrier(stack, commandHandle, attachmentDepth.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT,
                 VK13.VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT,
                 VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT, VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_DEPTH_BIT);

         VK13.vkCmdBeginRendering(commandHandle, renderingInfo);
         VK13.vkCmdBindPipeline(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         VulkanImage colorImage = attachmentColor.getImage();
         StructureUtils.setupViewportAndScissor(stack, colorImage.getWidth(), colorImage.getHeight(), commandHandle);

         VulkanUtils.copyMatrixToBuffer(device, allocationUtil, viewMatrixBuffers.get(currentFrame), scene.getCamera().getViewMatrix(), 0);
         LongBuffer descriptorSetsBuf = stack.mallocLong(4)
                         .put(0, allocator.getDescriptorSet(DESC_ID_PROJECTION).getId())
                         .put(1, allocator.getDescriptorSet(DESC_ID_VIEW, currentFrame).getId())
                         .put(2, allocator.getDescriptorSet(DESC_ID_MATERIALS).getId())
                         .put(3, allocator.getDescriptorSet(DESC_ID_TEXTURE).getId());
         VK13.vkCmdBindDescriptorSets(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayoutId(),
                 0, descriptorSetsBuf, null);

         renderEntities(stack, scene, commandBuffer, false);
         renderEntities(stack, scene, commandBuffer, true);

         VK13.vkCmdEndRendering(commandHandle);
      }
   }

   private void renderEntities(MemoryStack stack, VulkanScene scene, CommandBuffer commandBuffer, boolean doTransparency) {
      var commandHandle = commandBuffer.getCommandBuffer();
      ModelCache modelCache = ModelCache.getInstance();
      logger.trace("rendering entities - doTransparency: {}", doTransparency);
      scene.getEntities().forEach(e -> {
         VulkanModel model = modelCache.getModel(e.getModelId());
         model.bindMeshes(stack, commandHandle, pipeline.getLayoutId(), e.getModelMatrix(), doTransparency);
      });
   }

   public void resize(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanSwapChain swapChain, VulkanScene scene) {
      renderingInfo.free();
      attachmentInfoColor.free();
      attachmentInfoDepth.free();
      attachmentColor.cleanup(device, allocationUtil);
      attachmentDepth.cleanup(device, allocationUtil);

      attachmentColor = initColorAttachment(device, allocationUtil, swapChain);
      attachmentDepth = initDepthAttachment(swapChain, device, allocationUtil);
      attachmentInfoColor = initColorAttachmentInfo(attachmentColor, clearValueColor);
      attachmentInfoDepth = initDepthAttachmentInfo(attachmentDepth, clearValueDepth);
      renderingInfo = initRenderInfo(attachmentColor, attachmentInfoColor, attachmentInfoDepth);

      VulkanUtils.copyMatrixToBuffer(device, allocationUtil, projectionMatrixBuffer, scene.getProjection().getProjectionMatrix(), 0);
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

   public Attachment getAttachmentColor() {
      return attachmentColor;
   }
}
