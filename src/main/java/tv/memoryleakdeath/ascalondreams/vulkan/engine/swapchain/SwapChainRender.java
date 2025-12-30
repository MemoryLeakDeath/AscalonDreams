package tv.memoryleakdeath.ascalondreams.vulkan.engine.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRSynchronization2;
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
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSurface;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class SwapChainRender {
   private static final Logger logger = LoggerFactory.getLogger(SwapChainRender.class);
   private static final String DESC_ID_ATT = "FWD_DESC_ID_ATT";
   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/swapchain_fragment_shader.glsl";
   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/swapchain_vertex_shader.glsl";
   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";

   private final DescriptorSetLayout attributeLayout;
   private final VkClearValue clearValue;
   private final Pipeline pipeline;
   private final VulkanTextureSampler textureSampler;
   private List<VkRenderingAttachmentInfo.Buffer> colorAttachmentInfos;
   private List<VkRenderingInfo> renderingInfos;

   public SwapChainRender(LogicalDevice device, DescriptorAllocator allocator, VulkanSwapChain swapChain,
                          VulkanSurface surface, PipelineCache pipelineCache, Attachment sourceAttachment) {
      this.clearValue = VkClearValue.calloc()
              .color(c -> c.float32(0, 0f).float32(1, 0f).float32(2, 0f).float32(3, 0f));
      this.colorAttachmentInfos = initColorAttachmentInfos(swapChain, clearValue);
      this.renderingInfos = initRenderingInfos(swapChain, colorAttachmentInfos);
      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT, VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK,
              1, true);

      var attributeLayoutInfo = new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
              0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT);
      this.attributeLayout = new DescriptorSetLayout(device, attributeLayoutInfo);
      initAttributeDescriptorSet(device, allocator, attributeLayout, sourceAttachment, textureSampler);

      List<ShaderModule> shaderModules = initShaderModules(device);
      this.pipeline = initPipeline(device, surface, pipelineCache, shaderModules, List.of(attributeLayout));
      shaderModules.forEach(s -> s.cleanup(device));
   }

   private static void initAttributeDescriptorSet(LogicalDevice device, DescriptorAllocator allocator, DescriptorSetLayout layout,
                                                  Attachment attachment, VulkanTextureSampler sampler) {
      DescriptorSet descriptorSet = allocator.addDescriptorSets(device, DESC_ID_ATT, 1, layout).getFirst();
      descriptorSet.setImage(device, attachment.getImageView(), sampler, 0);
   }

   private static List<VkRenderingAttachmentInfo.Buffer> initColorAttachmentInfos(VulkanSwapChain chain, VkClearValue clear) {
      List<VkRenderingAttachmentInfo.Buffer> result = new ArrayList<>();
      int numImages = chain.getNumImages();
      for(int i = 0; i < numImages; i++) {
         var attachments = VkRenderingAttachmentInfo.calloc(1);
         attachments.get(0)
                 .sType$Default()
                 .imageView(chain.getImageView(i).getImageViewId())
                 .imageLayout(KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                 .clearValue(clear);
         result.add(attachments);
      }
      return result;
   }

   private static Pipeline initPipeline(LogicalDevice device, VulkanSurface surface, PipelineCache cache, List<ShaderModule> modules, List<DescriptorSetLayout> layouts) {
      var vertexBufferStructure = new EmptyVertexBufferStructure();
      var buildInfo = new PipelineBuildInfo(modules, vertexBufferStructure.getVertexInputStateCreateInfo(),
              new int[]{surface.getSurfaceFormat().imageFormat()}, VK13.VK_FORMAT_UNDEFINED, null, layouts, false, false);
      var pipeline = new Pipeline(device, cache, buildInfo);
      vertexBufferStructure.cleanup();
      return pipeline;
   }

   private static List<VkRenderingInfo> initRenderingInfos(VulkanSwapChain chain, List<VkRenderingAttachmentInfo.Buffer> colorAttachments) {
      List<VkRenderingInfo> result = new ArrayList<>();
      int numImages = chain.getNumImages();

      try(var stack = MemoryStack.stackPush()) {
         VkExtent2D extent = chain.getSwapChainExtent();
         var renderArea = VkRect2D.calloc(stack).extent(extent);

         for(int i = 0; i < numImages; i++) {
            var renderingInfo = VkRenderingInfo.calloc()
                    .sType$Default()
                    .renderArea(renderArea)
                    .layerCount(1)
                    .pColorAttachments(colorAttachments.get(i));
            result.add(renderingInfo);
         }
      }
      return result;
   }

   private static List<ShaderModule> initShaderModules(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, true);
      ShaderCompiler.compileShaderIfChanged(FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, true);
      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, VERTEX_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, FRAGMENT_SHADER_FILE_SPV, null));
   }

   public void cleanup(LogicalDevice device) {
      textureSampler.cleanup(device);
      attributeLayout.cleanup(device);
      pipeline.cleanup(device);
      renderingInfos.forEach(VkRenderingInfo::free);
      colorAttachmentInfos.forEach(VkRenderingAttachmentInfo.Buffer::free);
      clearValue.free();
   }

   public void render(VulkanSwapChain swapChain, DescriptorAllocator allocator, CommandBuffer cmdBuffer, Attachment sourceAttachment, int imageIndex) {
      try(var stack = MemoryStack.stackPush()) {
         long swapChainImage = swapChain.getImageView(imageIndex).getImageId();
         VkCommandBuffer cmdHandle = cmdBuffer.getCommandBuffer();

         StructureUtils.imageBarrier(stack, cmdHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_UNDEFINED, VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
                 VK13.VK_ACCESS_2_NONE, VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);

         StructureUtils.imageBarrier(stack, cmdHandle, sourceAttachment.getImage().getId(),
                 VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
                 VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK13.VK_ACCESS_2_SHADER_READ_BIT,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);

         VK13.vkCmdBeginRendering(cmdHandle, renderingInfos.get(imageIndex));
         VK13.vkCmdBindPipeline(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         StructureUtils.setupViewportAndScissor(stack, swapChain.getSwapChainExtent().width(),
                 swapChain.getSwapChainExtent().height(), cmdHandle);

         LongBuffer descriptorSets = stack.mallocLong(1)
                 .put(0, allocator.getDescriptorSet(DESC_ID_ATT).getId());
         VK13.vkCmdBindDescriptorSets(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS,
                 pipeline.getLayoutId(), 0, descriptorSets, null);

         VK13.vkCmdDraw(cmdHandle, 3, 1, 0, 0);
         VK13.vkCmdEndRendering(cmdHandle);

         StructureUtils.imageBarrier(stack, cmdHandle, swapChainImage,
                 VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                 VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT, VK13.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT,
                 VK13.VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT, VK13.VK_PIPELINE_STAGE_2_NONE,
                 VK13.VK_IMAGE_ASPECT_COLOR_BIT);
      }
   }

   public void resize(LogicalDevice device, VulkanSwapChain chain, DescriptorAllocator allocator, Attachment sourceAttachment) {
      renderingInfos.forEach(VkRenderingInfo::free);
      colorAttachmentInfos.forEach(VkRenderingAttachmentInfo.Buffer::free);
      colorAttachmentInfos = initColorAttachmentInfos(chain, clearValue);
      renderingInfos = initRenderingInfos(chain, colorAttachmentInfos);

      DescriptorSet descriptorSet = allocator.getDescriptorSet(DESC_ID_ATT);
      descriptorSet.setImage(device, sourceAttachment.getImageView(), textureSampler, 0);
   }
}
