package tv.memoryleakdeath.ascalondreams.render.skybox;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.buffers.GlobalBuffers;
import tv.memoryleakdeath.ascalondreams.cache.MaterialCache;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.cache.TextureCache;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.model.VulkanTexture;
import tv.memoryleakdeath.ascalondreams.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.pojo.PushConstantRange;
import tv.memoryleakdeath.ascalondreams.render.Attachment;
import tv.memoryleakdeath.ascalondreams.render.Pipeline;
import tv.memoryleakdeath.ascalondreams.render.Renderer;
import tv.memoryleakdeath.ascalondreams.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.render.lighting.MaterialAttachments;
import tv.memoryleakdeath.ascalondreams.render.postprocess.EmptyVertexBufferStructure;
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
import java.util.Collections;
import java.util.List;

public class SkyboxRenderer implements Renderer {
   private static final Logger logger = LoggerFactory.getLogger(SkyboxRenderer.class);
   private VkClearValue clearValueColor;
   private VkClearValue clearValueDepth;
   private final Pipeline pipeline;
   private VkRenderingAttachmentInfo.Buffer attachmentInfoColor;
   private VkRenderingAttachmentInfo attachmentInfoDepth;
   private MaterialAttachments materialAttachments;
   private VkRenderingInfo renderingInfo;
//   private final VulkanBuffer projectionMatrixBuffer;
   private final List<VulkanBuffer> viewMatrixBuffers;
//   private final DescriptorSetLayout fragmentStorageLayout;
//   private final DescriptorSetLayout textureLayout;
//   private final DescriptorSetLayout vertexUniformLayout;
//   private final VulkanTextureSampler textureSampler;
   private final ByteBuffer pushConstantsBuffer;


   private static final List<ShaderInfo> SKYBOX_SHADERS = List.of(
           new ShaderInfo("shaders/skybox/skybox_vertex_shader.glsl", Shaderc.shaderc_glsl_vertex_shader, VK13.VK_SHADER_STAGE_VERTEX_BIT),
           new ShaderInfo("shaders/skybox/skybox_fragment_shader.glsl", Shaderc.shaderc_glsl_fragment_shader, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
   private static final List<ShaderInfo> REFLECTION_SHADERS = List.of(
           new ShaderInfo("shaders/skybox/reflection_vertex_shader.glsl", Shaderc.shaderc_glsl_vertex_shader, VK13.VK_SHADER_STAGE_VERTEX_BIT),
           new ShaderInfo("shaders/skybox/reflection_fragment_shader.glsl", Shaderc.shaderc_glsl_fragment_shader, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
   private static final boolean DEBUG_SHADERS = true;
   private static final String DESC_ID_MATERIALS = "SCN_DESC_ID_MAT";
   private static final String DESC_ID_PROJECTION = "SCN_DESC_ID_PROJ";
   private static final String DESC_ID_TEXTURE = "SCN_DESC_ID_TEX";
   private static final String DESC_ID_VIEW = "SCN_DESC_ID_VIEW";
   private static final int PUSH_CONSTANTS_SIZE = VulkanConstants.PTR_SIZE * 2;
   private final VulkanTexture skyboxTexture;
   private final VulkanTextureSampler skyboxSampler;
   private final DescriptorSetLayout skyboxTextureLayout;
   private final DescriptorSetLayout skyboxUniformLayout;
   private final Pipeline reflectionPipeline;

   // singletons
   private VulkanSwapChain swapChain = VulkanSwapChain.getInstance();
   private PipelineCache cache = PipelineCache.getInstance();
   private LogicalDevice device = DeviceManager.getDevice();
   private DescriptorAllocator allocator = DescriptorAllocator.getInstance();
   private VulkanScene scene = VulkanScene.getInstance();
   private MemoryAllocationUtil allocationUtil = MemoryAllocationUtil.getInstance();
   private GlobalBuffers globalBuffers = GlobalBuffers.getInstance();
   private MaterialCache materialCache = MaterialCache.getInstance();
   private TextureCache textureCache = TextureCache.getInstance();

   private SkyboxRenderer() {
      this.clearValueColor = VkClearValue.calloc().color(
              c -> c.float32(0, 0f)
                      .float32(1, 0.0f)
                      .float32(2, 0.0f)
                      .float32(3, 1f));
      this.clearValueDepth = VkClearValue.calloc().color(c -> c.float32(0, 1f));
      this.materialAttachments = new MaterialAttachments(device, allocationUtil, swapChain);
      this.attachmentInfoColor = initColorAttachmentInfo(materialAttachments, clearValueColor);
      this.attachmentInfoDepth = initDepthAttachmentInfo(materialAttachments, clearValueDepth);
      this.renderingInfo = initRenderInfo(swapChain, attachmentInfoColor, attachmentInfoDepth);


      this.skyboxTexture = textureCache.getSkyboxTextures().getFirst();
      this.skyboxSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
              VK13.VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE, 1, true);
      this.skyboxUniformLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_VERTEX_BIT | VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
      this.skyboxTextureLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
              1, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
      this.viewMatrixBuffers = VulkanUtils.createHostVisibleBuffers(device, allocationUtil, allocator, VulkanConstants.MAT4X4_SIZE,
              VulkanConstants.MAX_IN_FLIGHT, VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_VIEW, skyboxUniformLayout);

      List<ShaderModule> shaderModules = createShaderModules(device, SKYBOX_SHADERS);
      this.pushConstantsBuffer = MemoryUtil.memAlloc(PUSH_CONSTANTS_SIZE);
      this.pipeline = createPipeline(device, shaderModules, cache);
      shaderModules.forEach(s -> s.cleanup(device));

      List<ShaderModule> reflectionShaderModules = createShaderModules(device, REFLECTION_SHADERS);
      this.reflectionPipeline = createPipeline(device, reflectionShaderModules, cache);
      reflectionShaderModules.forEach(s -> s.cleanup(device));
   }

   private static VkRenderingAttachmentInfo.Buffer initColorAttachmentInfo(MaterialAttachments attachment, VkClearValue clearValue) {
      List<Attachment> colorAttachments = attachment.getColorAttachments();
      int numAttachments = colorAttachments.size();
      var result = VkRenderingAttachmentInfo.calloc(numAttachments);
      for(int i = 0; i < numAttachments; i++) {
         result.get(i)
                 .sType$Default()
                 .imageView(colorAttachments.get(i).getImageView().getImageViewId())
                 .imageLayout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                 .clearValue(clearValue);
      }
      return result;
   }

   private static VkRenderingAttachmentInfo initDepthAttachmentInfo(MaterialAttachments attachmentDepth, VkClearValue clearValue) {
      return VkRenderingAttachmentInfo.calloc()
              .sType$Default()
              .imageView(attachmentDepth.getDepthAttachment().getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
              .clearValue(clearValue);
   }

   private static VkRenderingInfo initRenderInfo(VulkanSwapChain swapChain, VkRenderingAttachmentInfo.Buffer colorAttachments,
                               VkRenderingAttachmentInfo depthAttachments) {
      try(var stack = MemoryStack.stackPush()) {
         var extent = swapChain.getSwapChainExtent();
         var renderArea = VkRect2D.calloc(stack).extent(extent);
         return VkRenderingInfo.calloc()
                 .sType$Default()
                 .renderArea(renderArea)
                 .layerCount(1)
                 .pColorAttachments(colorAttachments)
                 .pDepthAttachment(depthAttachments);
      }
   }

   private Pipeline createPipeline(LogicalDevice device, List<ShaderModule> shaderModules, PipelineCache cache) {
      var vertexBufferStructure = new EmptyVertexBufferStructure();
      var vertexPcSize = VulkanConstants.MAT4X4_SIZE + VulkanConstants.PTR_SIZE * 2;
      var info = new PipelineBuildInfo(shaderModules, vertexBufferStructure.getVertexInputStateCreateInfo(),
              new int[] { MaterialAttachments.POSITION_FORMAT, MaterialAttachments.ALBEDO_FORMAT, MaterialAttachments.NORMAL_FORMAT, MaterialAttachments.PBR_FORMAT},
              MaterialAttachments.DEPTH_FORMAT, List.of(
                      new PushConstantRange(VK13.VK_SHADER_STAGE_VERTEX_BIT,0, vertexPcSize),
                      new PushConstantRange(VK13.VK_SHADER_STAGE_FRAGMENT_BIT, vertexPcSize, VulkanConstants.INT_SIZE)),
              List.of(skyboxUniformLayout, skyboxTextureLayout), true, false);
      var pipeline = new Pipeline(device, cache, info);
      vertexBufferStructure.cleanup();
      return pipeline;
   }

   private static List<ShaderModule> createShaderModules(LogicalDevice device, List<ShaderInfo> infoList) {
      List<ShaderModule> modules = new ArrayList<>();
      for(ShaderInfo info : infoList) {
         ShaderCompiler.compileShaderIfChanged(info.file(), info.shaderFlag(), DEBUG_SHADERS);
         modules.add(new ShaderModule(device, info.vulkanFlags(), info.file() + ".spv", null));
      }
      return modules;
   }

   @Override
   public void cleanup() {
      pipeline.cleanup(device);
      reflectionPipeline.cleanup(device);
      viewMatrixBuffers.forEach(b -> b.cleanup(device, allocationUtil));
//      fragmentStorageLayout.cleanup(device);
//      textureLayout.cleanup(device);
//      textureSampler.cleanup(device);
//      projectionMatrixBuffer.cleanup(device, allocationUtil);
//      vertexUniformLayout.cleanup(device);
      renderingInfo.free();
      attachmentInfoDepth.free();
      materialAttachments.cleanup(device, allocationUtil);
      attachmentInfoColor.free();
      MemoryUtil.memFree(pushConstantsBuffer);
      clearValueDepth.free();
      clearValueColor.free();
   }

   @Override
   public void load() {
      var descriptorSet = allocator.addDescriptorSet(device, DESC_ID_TEXTURE, skyboxTextureLayout);
      descriptorSet.setImage(device, skyboxTexture.getImageView(), skyboxSampler, 1);
   }

   @Override
   public void render(CommandBuffer commandBuffer, int currentFrame, int imageIndex) {
      render(commandBuffer, currentFrame);
   }

   private void render(CommandBuffer commandBuffer, int currentFrame) {
      try(var stack = MemoryStack.stackPush()) {
         var commandHandle = commandBuffer.getCommandBuffer();
         for(Attachment attachment : materialAttachments.getColorAttachments()) {
            StructureUtils.imageBarrier(stack, commandHandle, attachment.getImage().getId(),
                    VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                    VK13.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                    VK13.VK_ACCESS_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                    VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         }

         VK13.vkCmdBeginRendering(commandHandle, renderingInfo);
         VK13.vkCmdBindPipeline(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         StructureUtils.setupViewportAndScissor(stack, materialAttachments.getWidth(), materialAttachments.getHeight(), commandHandle);

         VulkanUtils.copyMatrixToBuffer(device, allocationUtil, viewMatrixBuffers.get(currentFrame), scene.getCamera().getViewMatrix(), 0);
         LongBuffer descriptorSetsBuf = stack.mallocLong(4)
                         .put(0, allocator.getDescriptorSet(DESC_ID_PROJECTION).getId())
                         .put(1, allocator.getDescriptorSet(DESC_ID_VIEW, currentFrame).getId())
                         .put(2, allocator.getDescriptorSet(DESC_ID_MATERIALS).getId())
                         .put(3, allocator.getDescriptorSet(DESC_ID_TEXTURE).getId());
         VK13.vkCmdBindDescriptorSets(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getLayoutId(),
                 0, descriptorSetsBuf, null);

         setPushConstants(commandHandle, globalBuffers, currentFrame);

         VK13.vkCmdDrawIndirect(commandHandle, globalBuffers.getIndirectBuffer(currentFrame).getBuffer(), 0,
                 globalBuffers.getDrawCount(currentFrame), GlobalBuffers.IND_COMMAND_STRIDE);

         VK13.vkCmdEndRendering(commandHandle);
      }
   }

   @Override
   public void resize() {
      renderingInfo.free();
      attachmentInfoDepth.free();
      materialAttachments.cleanup(device, allocationUtil);
      Collections.singletonList(attachmentInfoColor).forEach(VkRenderingAttachmentInfo.Buffer::free);

      materialAttachments = new MaterialAttachments(device, allocationUtil, swapChain);
      attachmentInfoColor = initColorAttachmentInfo(materialAttachments, clearValueColor);
      attachmentInfoDepth = initDepthAttachmentInfo(materialAttachments, clearValueDepth);
      renderingInfo = initRenderInfo(swapChain, attachmentInfoColor, attachmentInfoDepth);

//      VulkanUtils.copyMatrixToBuffer(device, allocationUtil, projectionMatrixBuffer, scene.getProjection().getProjectionMatrix(), 0);
   }

   public MaterialAttachments getMaterialAttachments() {
      return materialAttachments;
   }

   private void setPushConstants(VkCommandBuffer commandHandle, GlobalBuffers globalBuffers, int currentFrame) {
      int offset = 0;
      pushConstantsBuffer.putLong(offset, globalBuffers.getInstanceDataAddress(currentFrame));
      offset += VulkanConstants.PTR_SIZE;
      pushConstantsBuffer.putLong(offset, globalBuffers.getModelMatricesAddress(currentFrame));
      VK13.vkCmdPushConstants(commandHandle, pipeline.getLayoutId(), VK13.VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantsBuffer);
   }

   public static SkyboxRenderer getInstance() {
      return new SkyboxRenderer();
   }
}

record ShaderInfo(String file, int shaderFlag, int vulkanFlags) {}