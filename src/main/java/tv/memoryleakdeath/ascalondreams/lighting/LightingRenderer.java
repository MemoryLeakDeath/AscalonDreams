package tv.memoryleakdeath.ascalondreams.lighting;

import org.lwjgl.system.MemoryStack;
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
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.postprocess.EmptyVertexBufferStructure;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.Attachment;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.Pipeline;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.PipelineCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImage;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class LightingRenderer {
   private static final Logger logger = LoggerFactory.getLogger(LightingRenderer.class);
   private static final int COLOR_FORMAT = VK13.VK_FORMAT_R32G32B32A32_SFLOAT;
   private static final String DESC_ID_ATT = "LIGHT_DESC_ID_ATT";
   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/lighting_fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/lighting_vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";

   private final DescriptorSetLayout attachmentLayout;
   private final VkClearValue clearColor;
   private final Pipeline pipeline;
   private final VulkanTextureSampler textureSampler;
   private Attachment attachmentColor;
   private VkRenderingAttachmentInfo.Buffer attachmentColorInfo;
   private VkRenderingInfo renderingInfo;

   public LightingRenderer(LogicalDevice device, DescriptorAllocator allocator, MemoryAllocationUtil allocationUtil,
                           VulkanSwapChain swapChain, PipelineCache pipelineCache, List<Attachment> attachments) {
      this.clearColor = VkClearValue.calloc().color(c -> c.float32(0, 0f)
              .float32(1, 0f).float32(2, 0f).float32(3, 0f));
      this.attachmentColor = initColorAttachment(device, allocationUtil, swapChain);
      this.attachmentColorInfo = initColorAttachmentInfo(attachmentColor, clearColor);
      this.renderingInfo = initRenderingInfo(attachmentColor, attachmentColorInfo);

      List<ShaderModule> shaderModules = initShaderModules(device);
      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT,
              VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK, 1, true);
      List<DescriptorSetLayoutInfo> descriptorSetLayoutInfos = new ArrayList<>();
      for(int i = 0; i < attachments.size(); i++) {
         descriptorSetLayoutInfos.add(new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                 i, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
      }
      this.attachmentLayout = new DescriptorSetLayout(device, descriptorSetLayoutInfos);
      initAttachmentDescriptorSet(device, allocator, attachmentLayout, attachments, textureSampler);

      this.pipeline = initPipeline(device, pipelineCache, shaderModules, List.of(attachmentLayout));
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
              COLOR_FORMAT, VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);
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
      var info = new PipelineBuildInfo(modules, vertexBuff.getVertexInputStateCreateInfo(), COLOR_FORMAT,
              VK13.VK_FORMAT_UNDEFINED, null, layouts, true);
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

   private static List<ShaderModule> initShaderModules(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, true);
      ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, true);
      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV, null));
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      pipeline.cleanup(device);
      attachmentLayout.cleanup(device);
      textureSampler.cleanup(device);
      renderingInfo.free();
      attachmentColor.cleanup(device, allocationUtil);
      attachmentColorInfo.free();
      clearColor.free();
   }

   public void render(DescriptorAllocator allocator, CommandBuffer commandBuffer, MaterialAttachments materialAttachments) {
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

         VK13.vkCmdBeginRendering(commandHandle, renderingInfo);
         VK13.vkCmdBindPipeline(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());

         VulkanImage colorImage = attachmentColor.getImage();
         int width = colorImage.getWidth();
         int height = colorImage.getHeight();
         StructureUtils.setupViewportAndScissor(stack, width, height, commandHandle);

         LongBuffer descriptorSets = stack.mallocLong(1)
                 .put(0, allocator.getDescriptorSet(DESC_ID_ATT).getId());
         VK13.vkCmdBindDescriptorSets(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS,
                 pipeline.getLayoutId(), 0, descriptorSets, null);

         VK13.vkCmdDraw(commandHandle, 3, 1, 0, 0);
         VK13.vkCmdEndRendering(commandHandle);
      }
   }

   public void resize(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanSwapChain swapChain,
                      DescriptorAllocator allocator, List<Attachment> attachments) {
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

   public Attachment getAttachmentColor() {
      return attachmentColor;
   }
}
