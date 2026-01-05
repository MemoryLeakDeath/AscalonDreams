package tv.memoryleakdeath.ascalondreams.lighting;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.render.RenderChain;
import tv.memoryleakdeath.ascalondreams.render.Renderer;
import tv.memoryleakdeath.ascalondreams.render.SceneRenderer;
import tv.memoryleakdeath.ascalondreams.shadow.CascadeData;
import tv.memoryleakdeath.ascalondreams.shadow.CascadeShadows;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.postprocess.EmptyVertexBufferStructure;
import tv.memoryleakdeath.ascalondreams.render.Attachment;
import tv.memoryleakdeath.ascalondreams.render.Pipeline;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.render.VulkanImage;
import tv.memoryleakdeath.ascalondreams.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.shadow.ShadowRenderer;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.StructureUtils;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LightingRenderer implements Renderer {
   private static final Logger logger = LoggerFactory.getLogger(LightingRenderer.class);
   private static final int COLOR_FORMAT = VK13.VK_FORMAT_R32G32B32A32_SFLOAT;
   private static final String DESC_ID_ATT = "LIGHT_DESC_ID_ATT";
   private static final String DESC_ID_LIGHTS = "LIGHT_DESC_ID_LIGHTS";
   private static final String DESC_ID_SCENE = "LIGHT_DESC_ID_SCENE";
   private static final String DESC_ID_SHADOW_MATRICES = "LIGHT_DESC_ID_SHADOW_MATRICES";
   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/lighting_fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/lighting_vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";

   private final DescriptorSetLayout attachmentLayout;
   private final VkClearValue clearColor;
   private final LightSpecializationConstants lightSpecializationConstants = new LightSpecializationConstants();
   private final List<VulkanBuffer> lightingBuffers;
   private final Pipeline pipeline;
   private final List<VulkanBuffer> sceneBuffers;
   private final DescriptorSetLayout sceneLayout;
   private final List<VulkanBuffer> shadowMatrices;
   private final DescriptorSetLayout storageLayout;
   private final VulkanTextureSampler textureSampler;
   private Attachment attachmentColor;
   private VkRenderingAttachmentInfo.Buffer attachmentColorInfo;
   private VkRenderingInfo renderingInfo;

   // singletons
   private LogicalDevice device = DeviceManager.getDevice();
   private DescriptorAllocator allocator = DescriptorAllocator.getInstance();
   private MemoryAllocationUtil allocationUtil = MemoryAllocationUtil.getInstance();
   private VulkanSwapChain swapChain = VulkanSwapChain.getInstance();
   private PipelineCache pipelineCache = PipelineCache.getInstance();
   private VulkanScene scene = VulkanScene.getInstance();

   private LightingRenderer(List<Attachment> attachments) {
      this.clearColor = VkClearValue.calloc().color(c -> c.float32(0, 0f)
              .float32(1, 0f).float32(2, 0f).float32(3, 0f));
      this.attachmentColor = initColorAttachment(device, allocationUtil, swapChain);
      this.attachmentColorInfo = initColorAttachmentInfo(attachmentColor, clearColor);
      this.renderingInfo = initRenderingInfo(attachmentColor, attachmentColorInfo);

      List<ShaderModule> shaderModules = initShaderModules(device, lightSpecializationConstants);
      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT,
              VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK, 1, true);
      List<DescriptorSetLayoutInfo> descriptorSetLayoutInfos = new ArrayList<>();
      for(int i = 0; i < attachments.size() + 1; i++) {
         descriptorSetLayoutInfos.add(new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                 i, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
      }
      this.attachmentLayout = new DescriptorSetLayout(device, descriptorSetLayoutInfos);
      initAttachmentDescriptorSet(device, allocator, attachmentLayout, attachments, textureSampler);
      this.storageLayout = new DescriptorSetLayout(device,
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, 0, 1,
                      VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
      long bufferSize = (long)(VulkanConstants.VEC3_SIZE * 2 + VulkanConstants.INT_SIZE + VulkanConstants.FLOAT_SIZE) * VulkanScene.MAX_LIGHTS;
      this.lightingBuffers = VulkanUtils.createHostVisibleBuffers(device, allocationUtil, allocator, bufferSize, VulkanConstants.MAX_IN_FLIGHT,
              VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, DESC_ID_LIGHTS, storageLayout);
      this.sceneLayout = new DescriptorSetLayout(device,
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 0, 1,
                      VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
      bufferSize = VulkanConstants.VEC3_SIZE * 2 + VulkanConstants.FLOAT_SIZE + VulkanConstants.INT_SIZE + VulkanConstants.MAT4X4_SIZE;
      this.sceneBuffers = VulkanUtils.createHostVisibleBuffers(device, allocationUtil, allocator, bufferSize,
              VulkanConstants.MAX_IN_FLIGHT, VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_SCENE, sceneLayout);

      this.shadowMatrices = initShadowMatrixBuffers(device, allocationUtil, allocator, storageLayout);

      this.pipeline = initPipeline(device, pipelineCache, shaderModules, List.of(attachmentLayout, storageLayout, storageLayout, sceneLayout));
      shaderModules.forEach(s -> s.cleanup(device));
   }

   private static void initAttachmentDescriptorSet(LogicalDevice device, DescriptorAllocator allocator, DescriptorSetLayout layout,
                                                   List<Attachment> attachments, VulkanTextureSampler sampler) {
      DescriptorSet set = allocator.addDescriptorSet(device, DESC_ID_ATT, layout);
      List<VulkanImageView> imageViews = attachments.stream().map(Attachment::getImageView).toList();
      set.setImages(device, imageViews, sampler, 0);
   }

   private static Attachment initColorAttachment(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanSwapChain swapChain) {
      return new Attachment(device, allocationUtil, swapChain.getSwapChainExtent().width(), swapChain.getSwapChainExtent().height(),
              COLOR_FORMAT, VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, 1);
   }

   private static VkRenderingAttachmentInfo.Buffer initColorAttachmentInfo(Attachment attachment, VkClearValue clearColor) {
      return VkRenderingAttachmentInfo.calloc(1)
              .sType$Default()
              .imageView(attachment.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
              .clearValue(clearColor);
   }

   private static Pipeline initPipeline(LogicalDevice device, PipelineCache cache, List<ShaderModule> modules, List<DescriptorSetLayout> layouts) {
      var vertexBuff = new EmptyVertexBufferStructure();
      var info = new PipelineBuildInfo(modules, vertexBuff.getVertexInputStateCreateInfo(), new int[]{COLOR_FORMAT},
              VK13.VK_FORMAT_UNDEFINED, null, layouts, true, false);
      var pipeline = new Pipeline(device, cache, info);
      vertexBuff.cleanup();
      return pipeline;
   }

   private static VkRenderingInfo initRenderingInfo(Attachment attachmentColor, VkRenderingAttachmentInfo.Buffer attachmentColorInfo) {
      try(var stack = MemoryStack.stackPush()) {
         VkExtent2D extent = VkExtent2D.calloc(stack);
         extent.width(attachmentColor.getImage().getWidth());
         extent.height(attachmentColor.getImage().getHeight());
         var renderArea = VkRect2D.calloc(stack).extent(extent);

         return VkRenderingInfo.calloc()
                 .sType$Default()
                 .renderArea(renderArea)
                 .layerCount(1)
                 .pColorAttachments(attachmentColorInfo);
      }
   }

   private static List<ShaderModule> initShaderModules(LogicalDevice device, LightSpecializationConstants lightConsts) {
      ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, true);
      ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, true);
      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV, lightConsts.getSpecializationInfo()));
   }

   private static List<VulkanBuffer> initShadowMatrixBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil, DescriptorAllocator allocator, DescriptorSetLayout layout) {
      int numBuffers = VulkanConstants.MAX_IN_FLIGHT;
      VulkanBuffer[] buffers = new VulkanBuffer[numBuffers];
      List<DescriptorSet> descriptorSets = allocator.addDescriptorSets(device, DESC_ID_SHADOW_MATRICES, numBuffers, layout);
      for(int i = 0; i < numBuffers; i++) {
         long bufferSize = (long) (VulkanConstants.MAT4X4_SIZE + VulkanConstants.VEC4_SIZE) * VulkanScene.SHADOW_MAP_CASCADE_COUNT;
         buffers[i] = new VulkanBuffer(device, allocationUtil, bufferSize, VK13.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                 Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT,
                 VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
         descriptorSets.get(i).setBuffer(device, buffers[i], buffers[i].getRequestedSize(), layout.getLayoutInfo().binding(),
                 layout.getLayoutInfo().descriptorType());
      }
      return Arrays.stream(buffers).toList();
   }

   @Override
   public void cleanup() {
      storageLayout.cleanup(device);
      lightingBuffers.forEach(b -> b.cleanup(device, allocationUtil));
      sceneLayout.cleanup(device);
      sceneBuffers.forEach(b -> b.cleanup(device, allocationUtil));
      shadowMatrices.forEach(b -> b.cleanup(device, allocationUtil));
      pipeline.cleanup(device);
      attachmentLayout.cleanup(device);
      textureSampler.cleanup(device);
      lightSpecializationConstants.cleanup();
      renderingInfo.free();
      attachmentColor.cleanup(device, allocationUtil);
      attachmentColorInfo.free();
      clearColor.free();
   }

   @Override
   public void load() {
   }

   @Override
   public void render(CommandBuffer commandBuffer, int currentFrame, int imageIndex) {
      SceneRenderer sceneRenderer = RenderChain.getRendererInstance(SceneRenderer.class);
      ShadowRenderer shadowRenderer = RenderChain.getRendererInstance(ShadowRenderer.class);
      render(commandBuffer, sceneRenderer.getMaterialAttachments(), shadowRenderer.getColorAttachment(),
              shadowRenderer.getCascadeShadows(currentFrame), currentFrame);
   }

   private void render(CommandBuffer commandBuffer, MaterialAttachments materialAttachments,
                      Attachment shadowAttachment, CascadeShadows cascadeShadows, int currentFrame) {
      try (var stack = MemoryStack.stackPush()) {
         VkCommandBuffer commandHandle = commandBuffer.getCommandBuffer();
         StructureUtils.imageBarrier(stack, commandHandle, attachmentColor.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         for(Attachment attachment : materialAttachments.getColorAttachments()) {
            StructureUtils.imageBarrier(stack, commandHandle, attachment.getImage().getId(),
                    VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
                    VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK13.VK_ACCESS_2_SHADER_READ_BIT,
                    VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         }

         StructureUtils.imageBarrier(stack, commandHandle, shadowAttachment.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
                 VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK13.VK_ACCESS_2_SHADER_READ_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
         updateCascadeShadowMatrices(device, allocationUtil, cascadeShadows, currentFrame);

         VK13.vkCmdBeginRendering(commandHandle, renderingInfo);
         VK13.vkCmdBindPipeline(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());

         VulkanImage colorImage = attachmentColor.getImage();
         int width = colorImage.getWidth();
         int height = colorImage.getHeight();
         StructureUtils.setupViewportAndScissor(stack, width, height, commandHandle);

         LongBuffer descriptorSets = stack.mallocLong(4)
                 .put(0, allocator.getDescriptorSet(DESC_ID_ATT).getId())
                 .put(1, allocator.getDescriptorSet(DESC_ID_LIGHTS, currentFrame).getId())
                 .put(2, allocator.getDescriptorSet(DESC_ID_SHADOW_MATRICES, currentFrame).getId())
                 .put(3, allocator.getDescriptorSet(DESC_ID_SCENE, currentFrame).getId());

         updateSceneInfo(device, allocationUtil, scene, currentFrame);
         updateLights(device, allocationUtil, scene, currentFrame);

         VK13.vkCmdBindDescriptorSets(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS,
                 pipeline.getLayoutId(), 0, descriptorSets, null);

         VK13.vkCmdDraw(commandHandle, 3, 1, 0, 0);
         VK13.vkCmdEndRendering(commandHandle);
      }
   }

   @Override
   public void resize() {
      SceneRenderer sceneRenderer = RenderChain.getRendererInstance(SceneRenderer.class);
      ShadowRenderer shadowRenderer = RenderChain.getRendererInstance(ShadowRenderer.class);
      List<Attachment> attachments = new ArrayList<>(sceneRenderer.getMaterialAttachments().getColorAttachments());
      attachments.add(shadowRenderer.getColorAttachment());
      resize(attachments);
   }

   private void resize(List<Attachment> attachments) {
      renderingInfo.free();
      attachmentColorInfo.free();
      attachmentColor.cleanup(device, allocationUtil);

      attachmentColor = initColorAttachment(device, allocationUtil, swapChain);
      attachmentColorInfo = initColorAttachmentInfo(attachmentColor, clearColor);
      renderingInfo = initRenderingInfo(attachmentColor, attachmentColorInfo);

      DescriptorSet set = allocator.getDescriptorSet(DESC_ID_ATT);
      var imageViews = attachments.stream().map(Attachment::getImageView).toList();
      set.setImages(device, imageViews, textureSampler, 0);
   }

   private void updateCascadeShadowMatrices(LogicalDevice device, MemoryAllocationUtil allocationUtil, CascadeShadows cascadeShadows, int currentFrame) {
      VulkanBuffer buffer = shadowMatrices.get(currentFrame);
      long mappedMemory = buffer.map(device, allocationUtil);
      ByteBuffer dataBuf = MemoryUtil.memByteBuffer(mappedMemory, (int) buffer.getRequestedSize());
      int offset = 0;
      for(CascadeData data : cascadeShadows.getCascadeData()) {
         data.getProjectionViewMatrix().get(offset, dataBuf);
         dataBuf.putFloat(offset + VulkanConstants.MAT4X4_SIZE, data.getSplitDistance());
         offset += VulkanConstants.MAT4X4_SIZE + VulkanConstants.VEC4_SIZE;
      }
      buffer.unMap(device, allocationUtil);
   }

   private void updateLights(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanScene scene, int currentFrame) {
      List<Light> lights = scene.getLights();
      var lightBuf = lightingBuffers.get(currentFrame);
      long mappedMemory = lightBuf.map(device, allocationUtil);
      ByteBuffer dataBuf = MemoryUtil.memByteBuffer(mappedMemory, (int) lightBuf.getRequestedSize());

      int offset = 0;
      for(Light light : lights) {
         light.position().get(offset, dataBuf);
         offset += VulkanConstants.VEC3_SIZE;
         dataBuf.putInt(offset, light.directional() ? 1 : 0);
         offset += VulkanConstants.INT_SIZE;
         dataBuf.putFloat(offset, light.intensity());
         offset += VulkanConstants.FLOAT_SIZE;
         light.color().get(offset, dataBuf);
         offset += VulkanConstants.VEC3_SIZE;
      }
      lightBuf.unMap(device, allocationUtil);
   }

   private void updateSceneInfo(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanScene scene, int currentFrame) {
      VulkanBuffer sceneBuf = sceneBuffers.get(currentFrame);
      long mappedMemory = sceneBuf.map(device, allocationUtil);
      ByteBuffer dataBuf = MemoryUtil.memByteBuffer(mappedMemory, (int) sceneBuf.getRequestedSize());

      int offset = 0;
      scene.getCamera().getPosition().get(offset, dataBuf);
      offset += VulkanConstants.VEC3_SIZE;
      dataBuf.putFloat(offset, scene.getAmbientLightIntensity());
      offset += VulkanConstants.FLOAT_SIZE;
      scene.getAmbientLightColor().get(offset, dataBuf);
      offset += VulkanConstants.VEC3_SIZE;
      int numLights = (scene.getLights() != null) ? scene.getLights().size() : 0;
      dataBuf.putInt(offset, numLights);
      offset += VulkanConstants.INT_SIZE;
      scene.getCamera().getViewMatrix().get(offset, dataBuf);
      sceneBuf.unMap(device, allocationUtil);
   }

   public Attachment getAttachmentColor() {
      return attachmentColor;
   }

   public static LightingRenderer getInstance() {
      SceneRenderer sceneRenderer = RenderChain.getRendererInstance(SceneRenderer.class);
      ShadowRenderer shadowRenderer = RenderChain.getRendererInstance(ShadowRenderer.class);
      List<Attachment> shadowAttachments = new ArrayList<>(sceneRenderer.getMaterialAttachments().getColorAttachments());
      shadowAttachments.add(shadowRenderer.getColorAttachment());
      return new LightingRenderer(shadowAttachments);
   }
}
