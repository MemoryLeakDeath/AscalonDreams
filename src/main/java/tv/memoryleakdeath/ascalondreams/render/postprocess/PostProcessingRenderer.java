package tv.memoryleakdeath.ascalondreams.render.postprocess;

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
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.render.lighting.LightingRenderer;
import tv.memoryleakdeath.ascalondreams.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.render.Attachment;
import tv.memoryleakdeath.ascalondreams.render.Pipeline;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.render.RenderChain;
import tv.memoryleakdeath.ascalondreams.render.Renderer;
import tv.memoryleakdeath.ascalondreams.render.VulkanImage;
import tv.memoryleakdeath.ascalondreams.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.StructureUtils;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.List;

public class PostProcessingRenderer implements Renderer {
   public static final Logger logger = LoggerFactory.getLogger(PostProcessingRenderer.class);
   public static final int COLOR_FORMAT = VK13.VK_FORMAT_R16G16B16A16_SFLOAT;
   private static final String DESC_ID_ATT = "POST_DESC_ID_ATT";
   private static final String DESC_ID_SCREEN_SIZE = "POST_DESC_ID_SCREEN_SIZE";
   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/postprocess_fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/postprocess_vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";

   private final DescriptorSetLayout attributeLayout;
   private final VkClearValue clearColor;
   private final DescriptorSetLayout uniformFragmentLayout;
   private final Pipeline pipeline;
   private final VulkanBuffer screenSizeBuffer;
   private final SpecializationConstants specializationConstants = new SpecializationConstants();
   private final VulkanTextureSampler textureSampler;
   private Attachment colorAttachment;
   private VkRenderingAttachmentInfo.Buffer colorAttachmentInfo;
   private VkRenderingInfo renderingInfo;

   // singletons
   private LogicalDevice device = DeviceManager.getDevice();
   private MemoryAllocationUtil allocationUtil = MemoryAllocationUtil.getInstance();
   private DescriptorAllocator allocator = DescriptorAllocator.getInstance();
   private VulkanSwapChain swapChain = VulkanSwapChain.getInstance();
   private PipelineCache pipelineCache = PipelineCache.getInstance();

   private PostProcessingRenderer(Attachment sourceAttachment) {
      this.clearColor = VkClearValue.calloc();
      clearColor.color(c -> c.float32(0, 0f).float32(1, 0f).float32(2, 0f).float32(3, 0f));

      this.colorAttachment = initColorAttachment(device, swapChain, allocationUtil);
      this.colorAttachmentInfo = initColorAttachmentInfo(colorAttachment, clearColor);
      this.renderingInfo = initRenderingInfo(colorAttachment, colorAttachmentInfo);

      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT, VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK,
              1, true);
      var layoutInfo = new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 0, 1,
              VK13.VK_SHADER_STAGE_FRAGMENT_BIT);
      this.attributeLayout = new DescriptorSetLayout(device, layoutInfo);
      initAttributeDescriptorSet(device, allocator, attributeLayout, sourceAttachment, textureSampler);

      var layoutInfoFragmentUniform = new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 0, 1,
              VK13.VK_SHADER_STAGE_FRAGMENT_BIT);
      this.uniformFragmentLayout = new DescriptorSetLayout(device, layoutInfoFragmentUniform);
      this.screenSizeBuffer = VulkanUtils.createHostVisibleBuffer(device, allocationUtil, allocator, VulkanConstants.VEC2_SIZE,
              VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_SCREEN_SIZE, uniformFragmentLayout);
      createScreenSizeBuffer(device, allocationUtil, swapChain);

      List<ShaderModule> shaderModules = createShaderModules(device, specializationConstants);
      this.pipeline = initPipeline(device, pipelineCache, shaderModules, List.of(attributeLayout, uniformFragmentLayout));
      shaderModules.forEach(s -> s.cleanup(device));
   }

   private static void initAttributeDescriptorSet(LogicalDevice device, DescriptorAllocator allocator, DescriptorSetLayout layout,
                                                  Attachment attachment, VulkanTextureSampler sampler) {
      DescriptorSet descriptorSet = allocator.addDescriptorSets(device, DESC_ID_ATT, 1, layout).getFirst();
      descriptorSet.setImage(device, attachment.getImageView(), sampler, 0);
   }

   private static Attachment initColorAttachment(LogicalDevice device, VulkanSwapChain swapChain, MemoryAllocationUtil allocationUtil) {
      VkExtent2D extent = swapChain.getSwapChainExtent();
      return new Attachment(device, allocationUtil, extent.width(), extent.height(), COLOR_FORMAT, VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, 1);
   }

   private static VkRenderingAttachmentInfo.Buffer initColorAttachmentInfo(Attachment attachment, VkClearValue clear) {
      return VkRenderingAttachmentInfo.calloc(1)
              .sType$Default()
              .imageView(attachment.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
              .clearValue(clear);
   }

   private static VkRenderingInfo initRenderingInfo(Attachment colorAttachment, VkRenderingAttachmentInfo.Buffer colorAttachmentInfo) {
      try(var stack = MemoryStack.stackPush()) {
         VulkanImage image = colorAttachment.getImage();
         VkExtent2D extent = VkExtent2D.calloc(stack).width(image.getWidth()).height(image.getHeight());
         var renderArea = VkRect2D.calloc(stack).extent(extent);

         return VkRenderingInfo.calloc()
                 .sType$Default()
                 .renderArea(renderArea)
                 .layerCount(1)
                 .pColorAttachments(colorAttachmentInfo);
      }
   }

   private static Pipeline initPipeline(LogicalDevice device, PipelineCache cache, List<ShaderModule> modules, List<DescriptorSetLayout> layouts) {
      var vertexBufferStructure = new EmptyVertexBufferStructure();
      var info = new PipelineBuildInfo(modules, vertexBufferStructure.getVertexInputStateCreateInfo(),
              new int[] {COLOR_FORMAT}, VK13.VK_FORMAT_UNDEFINED, null, layouts, false, false);
      var pipeline = new Pipeline(device, cache, info);
      vertexBufferStructure.cleanup();
      return pipeline;
   }

   private void createScreenSizeBuffer(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanSwapChain chain) {
      long mem = screenSizeBuffer.map(device, allocationUtil);
      FloatBuffer buf = MemoryUtil.memFloatBuffer(mem, (int) screenSizeBuffer.getRequestedSize());
      VkExtent2D chainExtent = chain.getSwapChainExtent();
      buf.put(0, chainExtent.width());
      buf.put(1, chainExtent.height());
      screenSizeBuffer.unMap(device, allocationUtil);
   }

   private static List<ShaderModule> createShaderModules(LogicalDevice device, SpecializationConstants constants) {
      ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, true);
      ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, true);
      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV, constants.getSpecializationInfo()));
   }

   @Override
   public void cleanup() {
      clearColor.free();
      colorAttachment.cleanup(device, allocationUtil);
      textureSampler.cleanup(device);
      attributeLayout.cleanup(device);
      uniformFragmentLayout.cleanup(device);
      pipeline.cleanup(device);
      renderingInfo.free();
      colorAttachmentInfo.free();
      screenSizeBuffer.cleanup(device, allocationUtil);
      specializationConstants.cleanup();
   }

   @Override
   public void load() {
   }

   @Override
   public void render(CommandBuffer commandBuffer, int currentFrame, int imageIndex) {
      LightingRenderer lightingRenderer = RenderChain.getRendererInstance(LightingRenderer.class);
      render(commandBuffer, lightingRenderer.getAttachmentColor());
   }

   private void render(CommandBuffer cmdBuffer, Attachment sourceAttachment) {
      try(var stack = MemoryStack.stackPush()) {
         VkCommandBuffer cmdHandle = cmdBuffer.getCommandBuffer();
         StructureUtils.imageBarrier(stack, cmdHandle, sourceAttachment.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
                 VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK13.VK_ACCESS_2_SHADER_READ_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);

         StructureUtils.imageBarrier(stack, cmdHandle, colorAttachment.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);

         VK13.vkCmdBeginRendering(cmdHandle, renderingInfo);
         VK13.vkCmdBindPipeline(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         StructureUtils.setupViewportAndScissor(stack, swapChain.getSwapChainExtent().width(), swapChain.getSwapChainExtent().height(), cmdHandle);

         LongBuffer descriptorSets = stack.mallocLong(2)
                 .put(0, allocator.getDescriptorSet(DESC_ID_ATT).getId())
                 .put(1, allocator.getDescriptorSet(DESC_ID_SCREEN_SIZE).getId());
         VK13.vkCmdBindDescriptorSets(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS,
                 pipeline.getLayoutId(), 0, descriptorSets, null);

         VK13.vkCmdDraw(cmdHandle, 3, 1, 0, 0);
         VK13.vkCmdEndRendering(cmdHandle);
      }
   }

   @Override
   public void resize() {
      LightingRenderer lightingRenderer = RenderChain.getRendererInstance(LightingRenderer.class);
      resize(lightingRenderer.getAttachmentColor());
   }

   private void resize(Attachment sourceAttachment) {
      renderingInfo.free();
      colorAttachment.cleanup(device, allocationUtil);
      colorAttachmentInfo.free();
      colorAttachment = initColorAttachment(device, swapChain, allocationUtil);
      colorAttachmentInfo = initColorAttachmentInfo(colorAttachment, clearColor);
      renderingInfo = initRenderingInfo(colorAttachment, colorAttachmentInfo);

      DescriptorSet descriptorSet = allocator.getDescriptorSet(DESC_ID_ATT);
      descriptorSet.setImage(device, sourceAttachment.getImageView(), textureSampler, 0);
      createScreenSizeBuffer(device, allocationUtil, swapChain);
   }

   public Attachment getColorAttachment() {
      return colorAttachment;
   }

   public static PostProcessingRenderer getInstance() {
      LightingRenderer lightingRenderer = RenderChain.getRendererInstance(LightingRenderer.class);
      return new PostProcessingRenderer(lightingRenderer.getAttachmentColor());
   }
}
