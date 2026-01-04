package tv.memoryleakdeath.ascalondreams.shadow;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.animations.AnimationCache;
import tv.memoryleakdeath.ascalondreams.buffers.GlobalBuffers;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.cache.MaterialCache;
import tv.memoryleakdeath.ascalondreams.cache.ModelCache;
import tv.memoryleakdeath.ascalondreams.cache.TextureCache;
import tv.memoryleakdeath.ascalondreams.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.model.VulkanTexture;
import tv.memoryleakdeath.ascalondreams.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.pojo.PushConstantRange;
import tv.memoryleakdeath.ascalondreams.postprocess.EmptyVertexBufferStructure;
import tv.memoryleakdeath.ascalondreams.render.Attachment;
import tv.memoryleakdeath.ascalondreams.render.Pipeline;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.StructureUtils;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class ShadowRenderer {
   private static final Logger logger = LoggerFactory.getLogger(ShadowRenderer.class);
   public static final int DEPTH_FORMAT = VK13.VK_FORMAT_D32_SFLOAT;
   private static final int ATTACHMENT_FORMAT = VK13.VK_FORMAT_R32G32_SFLOAT;
   private static final String DESC_ID_MAT = "SHADOW_DESC_ID_MAT";
   private static final String DESC_ID_PRJ = "SHADOW_DESC_ID_PRJ";
   private static final String DESC_ID_TEXT = "SHADOW_SCN_DESC_ID_TEXT";
   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/shadow_fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final int PUSH_CONSTANTS_SIZE = VulkanConstants.PTR_SIZE * 2;
   private static final String SHADOW_GEOMETRY_SHADER_FILE_GLSL = "shaders/shadow_geometry_shader.glsl";
   private static final String SHADOW_GEOMETRY_SHADER_FILE_SPV = SHADOW_GEOMETRY_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/shadow_vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";

   private final List<CascadeShadows> cascadeShadows;
   private final VkClearValue clearColor;
   private final VkClearValue clearDepth;
   private final Attachment colorAttachment;
   private final VkRenderingAttachmentInfo.Buffer colorAttachmentInfo;
   private final Attachment depthAttachment;
   private final VkRenderingAttachmentInfo depthAttachmentInfo;
   private final DescriptorSetLayout fragmentStorageLayout;
   private final Pipeline pipeline;
   private final List<VulkanBuffer> projectionBuffers;
   private final ByteBuffer pushConstantBuffer;
   private final VkRenderingInfo renderingInfo;
   private final DescriptorSetLayout textureLayout;
   private final VulkanTextureSampler textureSampler;
   private final DescriptorSetLayout geometryUniformLayout;

   public ShadowRenderer(LogicalDevice device, MemoryAllocationUtil allocationUtil, DescriptorAllocator allocator, PipelineCache pipelineCache) {
      this.clearDepth = VkClearValue.calloc().color(c -> c.float32(0, 1f));
      this.clearColor = VkClearValue.calloc().color(c -> c.float32(0, 1f).float32(1, 1f));
      depthAttachment = initDepthAttachment(device, allocationUtil);
      this.depthAttachmentInfo = initDepthAttachmentInfo(depthAttachment, clearDepth);
      this.colorAttachment = initColorAttachment(device, allocationUtil);
      this.colorAttachmentInfo = initColorAttachmentInfo(colorAttachment, clearColor);
      this.pushConstantBuffer = MemoryUtil.memAlloc(PUSH_CONSTANTS_SIZE);
      this.renderingInfo = initRenderInfo(colorAttachmentInfo, depthAttachmentInfo);
      List<ShaderModule> shaderModules = initShaderModules(device);

      this.geometryUniformLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_GEOMETRY_BIT));
      long bufSize = (long)VulkanConstants.MAT4X4_SIZE * VulkanScene.SHADOW_MAP_CASCADE_COUNT;
      this.projectionBuffers = VulkanUtils.createHostVisibleBuffers(device, allocationUtil, allocator, bufSize,
              VulkanConstants.MAX_IN_FLIGHT, VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_PRJ, geometryUniformLayout);

      this.fragmentStorageLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));

      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT, VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK,
              1, true);
      this.textureLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
              0, TextureCache.MAX_TEXTURES, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));

      this.pipeline = initPipeline(device, pipelineCache, shaderModules,
              List.of(geometryUniformLayout, textureLayout, fragmentStorageLayout));
      shaderModules.forEach(s -> s.cleanup(device));

      this.cascadeShadows = new ArrayList<>();
      for(int i = 0; i < VulkanConstants.MAX_IN_FLIGHT; i++) {
         cascadeShadows.add(new CascadeShadows());
      }
   }

   private static Attachment initColorAttachment(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      return new Attachment(device, allocationUtil, ShadowUtils.SHADOW_MAP_SIZE, ShadowUtils.SHADOW_MAP_SIZE,
              ATTACHMENT_FORMAT, VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, VulkanScene.SHADOW_MAP_CASCADE_COUNT);
   }

   private static VkRenderingAttachmentInfo.Buffer initColorAttachmentInfo(Attachment sourceAttachment, VkClearValue clearValue) {
      return VkRenderingAttachmentInfo.calloc(1)
              .sType$Default()
              .imageView(sourceAttachment.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
              .clearValue(clearValue);
   }

   private static Attachment initDepthAttachment(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      return new Attachment(device, allocationUtil, ShadowUtils.SHADOW_MAP_SIZE, ShadowUtils.SHADOW_MAP_SIZE,
              DEPTH_FORMAT, VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VulkanScene.SHADOW_MAP_CASCADE_COUNT);
   }

   private static VkRenderingAttachmentInfo initDepthAttachmentInfo(Attachment depthAttachment, VkClearValue clearValue) {
      return VkRenderingAttachmentInfo.calloc()
              .sType$Default()
              .imageView(depthAttachment.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
              .clearValue(clearValue);
   }

   private static Pipeline initPipeline(LogicalDevice device, PipelineCache cache, List<ShaderModule> shaderModules, List<DescriptorSetLayout> layouts) {
      var vertexBuffer = new EmptyVertexBufferStructure();
      var info = new PipelineBuildInfo(shaderModules, vertexBuffer.getVertexInputStateCreateInfo(),
              new int[] {ATTACHMENT_FORMAT}, DEPTH_FORMAT,
              List.of(new PushConstantRange(VK13.VK_SHADER_STAGE_VERTEX_BIT, 0, PUSH_CONSTANTS_SIZE)),
              layouts, false, device.isDepthClamp());
      var pipeline = new Pipeline(device, cache, info);
      vertexBuffer.cleanup();
      return pipeline;
   }

   private static VkRenderingInfo initRenderInfo(VkRenderingAttachmentInfo.Buffer colorAttachmentInfo,
                                                 VkRenderingAttachmentInfo depthAttachments) {
      var result = VkRenderingInfo.calloc().sType$Default();
      try(var stack = MemoryStack.stackPush()) {
         VkExtent2D extent = VkExtent2D.calloc(stack)
                 .width(ShadowUtils.SHADOW_MAP_SIZE)
                 .height(ShadowUtils.SHADOW_MAP_SIZE);
         var renderArea = VkRect2D.calloc(stack).extent(extent);
         result.renderArea(renderArea)
                 .layerCount(VulkanScene.SHADOW_MAP_CASCADE_COUNT)
                 .pColorAttachments(colorAttachmentInfo)
                 .pDepthAttachment(depthAttachments);
      }
      return result;
   }

   private static List<ShaderModule> initShaderModules(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, true);
      ShaderCompiler.compileShaderIfChanged(SHADOW_GEOMETRY_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_geometry_shader, true);
      ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, true);

      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_GEOMETRY_BIT, SHADOW_GEOMETRY_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV, null));
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      pipeline.cleanup(device);
      geometryUniformLayout.cleanup(device);
      fragmentStorageLayout.cleanup(device);
      textureLayout.cleanup(device);
      textureSampler.cleanup(device);
      projectionBuffers.forEach(b -> b.cleanup(device, allocationUtil));
      renderingInfo.free();
      depthAttachmentInfo.free();
      depthAttachment.cleanup(device, allocationUtil);
      colorAttachmentInfo.free();
      colorAttachment.cleanup(device, allocationUtil);
      MemoryUtil.memFree(pushConstantBuffer);
      clearColor.free();
      clearDepth.free();
   }

   public CascadeShadows getCascadeShadows(int currentFrame) {
      return cascadeShadows.get(currentFrame);
   }

   public Attachment getColorAttachment() {
      return colorAttachment;
   }

   public void loadMaterials(LogicalDevice device, DescriptorAllocator allocator, MaterialCache materialCache, TextureCache textureCache) {
      DescriptorSet set = allocator.addDescriptorSet(device, DESC_ID_MAT, fragmentStorageLayout);
      var layoutInfo = fragmentStorageLayout.getLayoutInfo();
      var buffer = materialCache.getMaterialsBuffer();
      set.setBuffer(device, buffer, buffer.getRequestedSize(), layoutInfo.binding(), layoutInfo.descriptorType());

      List<VulkanImageView> imageViews = textureCache.getAsList().stream().map(VulkanTexture::getImageView).toList();
      var textureSet = allocator.addDescriptorSet(device, DESC_ID_TEXT, textureLayout);
      textureSet.setImagesArray(device, imageViews, textureSampler, 0);
   }

   public void render(LogicalDevice device, MemoryAllocationUtil allocationUtil, DescriptorAllocator allocator,
                      VulkanScene scene, CommandBuffer cmdBuffer,
                      ModelCache modelCache, MaterialCache materialCache, int currentFrame, GlobalBuffers globalBuffers) {
      AnimationCache animationCache = AnimationCache.getInstance();
      try(var stack = MemoryStack.stackPush()) {
         ShadowUtils.updateCascadeShadows(cascadeShadows.get(currentFrame), scene);
         VkCommandBuffer cmdHandle = cmdBuffer.getCommandBuffer();
         StructureUtils.imageBarrier(stack, cmdHandle, colorAttachment.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         VK13.vkCmdBeginRendering(cmdHandle, renderingInfo);
         VK13.vkCmdBindPipeline(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         StructureUtils.setupViewportAndScissor(stack, ShadowUtils.SHADOW_MAP_SIZE, ShadowUtils.SHADOW_MAP_SIZE, cmdHandle);

         updateProjectionBuffer(device, allocationUtil, currentFrame);
         LongBuffer descriptorSets = stack.mallocLong(3)
                 .put(0, allocator.getDescriptorSet(DESC_ID_PRJ, currentFrame).getId())
                 .put(1, allocator.getDescriptorSet(DESC_ID_TEXT).getId())
                 .put(2, allocator.getDescriptorSet(DESC_ID_MAT).getId());
         VK13.vkCmdBindDescriptorSets(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayoutId(),
                 0, descriptorSets, null);

         setPushConstants(cmdHandle, globalBuffers, currentFrame);
         VK13.vkCmdDrawIndirect(cmdHandle, globalBuffers.getIndirectBuffer(currentFrame).getBuffer(), 0,
                 globalBuffers.getDrawCount(currentFrame), GlobalBuffers.IND_COMMAND_STRIDE);
         VK13.vkCmdEndRendering(cmdHandle);
      }
   }

   private void setPushConstants(VkCommandBuffer cmdHandle, GlobalBuffers globalBuffers, int currentFrame) {
      int offset = 0;
      pushConstantBuffer.putLong(offset, globalBuffers.getInstanceDataAddress(currentFrame));
      offset += VulkanConstants.PTR_SIZE;
      pushConstantBuffer.putLong(offset, globalBuffers.getModelMatricesAddress(currentFrame));
      VK13.vkCmdPushConstants(cmdHandle, pipeline.getLayoutId(), VK13.VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantBuffer);
   }

   private void updateProjectionBuffer(LogicalDevice device, MemoryAllocationUtil allocationUtil, int currentFrame) {
      int offset = 0;
      List<CascadeData> cascadeDataList = cascadeShadows.get(currentFrame).getCascadeData();
      VulkanBuffer buf = projectionBuffers.get(currentFrame);
      long mappedMemory = buf.map(device, allocationUtil);
      ByteBuffer memBuf = MemoryUtil.memByteBuffer(mappedMemory, (int) buf.getRequestedSize());
      for(CascadeData data : cascadeDataList) {
         data.getProjectionViewMatrix().get(offset, memBuf);
         offset += VulkanConstants.MAT4X4_SIZE;
      }
      buf.unMap(device, allocationUtil);
   }
}
