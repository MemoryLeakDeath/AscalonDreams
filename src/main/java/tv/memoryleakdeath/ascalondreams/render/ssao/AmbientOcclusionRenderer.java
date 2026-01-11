package tv.memoryleakdeath.ascalondreams.render.ssao;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.buffers.GlobalBuffers;
import tv.memoryleakdeath.ascalondreams.cache.MaterialCache;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.cache.TextureCache;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSet;
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
import tv.memoryleakdeath.ascalondreams.render.VulkanImageView;
import tv.memoryleakdeath.ascalondreams.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.render.lighting.MaterialAttachments;
import tv.memoryleakdeath.ascalondreams.render.postprocess.EmptyVertexBufferStructure;
import tv.memoryleakdeath.ascalondreams.render.shadow.CascadeData;
import tv.memoryleakdeath.ascalondreams.render.shadow.CascadeShadows;
import tv.memoryleakdeath.ascalondreams.render.shadow.ShadowUtils;
import tv.memoryleakdeath.ascalondreams.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderInfo;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.StructureUtils;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

public class AmbientOcclusionRenderer implements Renderer {
   private static final Logger logger = LoggerFactory.getLogger(AmbientOcclusionRenderer.class);
//   public static final int DEPTH_FORMAT = VK13.VK_FORMAT_D32_SFLOAT;
//   private static final int ATTACHMENT_FORMAT = VK13.VK_FORMAT_R32G32_SFLOAT;
//   private static final String DESC_ID_MAT = "SHADOW_DESC_ID_MAT";
//   private static final String DESC_ID_PRJ = "SHADOW_DESC_ID_PRJ";
//   private static final String DESC_ID_TEXT = "SHADOW_SCN_DESC_ID_TEXT";
//   private static final String FRAGMENT_SHADER_FILE_GLSL = "shaders/shadow_fragment_shader.glsl";
//   private static final String FRAGMENT_SHADER_FILE_SPV = FRAGMENT_SHADER_FILE_GLSL + ".spv";
//   private static final int PUSH_CONSTANTS_SIZE = VulkanConstants.PTR_SIZE * 2;
//   private static final String SHADOW_GEOMETRY_SHADER_FILE_GLSL = "shaders/shadow_geometry_shader.glsl";
//   private static final String SHADOW_GEOMETRY_SHADER_FILE_SPV = SHADOW_GEOMETRY_SHADER_FILE_GLSL + ".spv";
//   private static final String VERTEX_SHADER_FILE_GLSL = "shaders/shadow_vertex_shader.glsl";
//   private static final String VERTEX_SHADER_FILE_SPV = VERTEX_SHADER_FILE_GLSL + ".spv";
   private static final List<ShaderInfo> SHADERS = List.of(
           new ShaderInfo("shaders/ssao/blur_fragment.glsl", VulkanConstants.FRAGMENT_SHADER_TYPE, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, true),
           new ShaderInfo("shaders/ssao/composition_fragment.glsl", VulkanConstants.FRAGMENT_SHADER_TYPE, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, true),
           new ShaderInfo("shaders/ssao/fullscreen_vertex.glsl", VulkanConstants.VERTEX_SHADER_TYPE, VK13.VK_SHADER_STAGE_VERTEX_BIT, true),
           new ShaderInfo("shaders/ssao/gbuffer_fragment.glsl", VulkanConstants.FRAGMENT_SHADER_TYPE, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, true),
           new ShaderInfo("shaders/ssao/gbuffer_vertex.glsl", VulkanConstants.VERTEX_SHADER_TYPE, VK13.VK_SHADER_STAGE_VERTEX_BIT, true)
   );

//   private final List<CascadeShadows> cascadeShadows;
//   private final VkClearValue clearColor;
//   private final VkClearValue clearDepth;
//   private final Attachment colorAttachment;
//   private final VkRenderingAttachmentInfo.Buffer colorAttachmentInfo;
//   private final Attachment depthAttachment;
//   private final VkRenderingAttachmentInfo depthAttachmentInfo;
//   private final DescriptorSetLayout fragmentStorageLayout;
//   private final Pipeline pipeline;
//   private final List<VulkanBuffer> projectionBuffers;
//   private final ByteBuffer pushConstantBuffer;
//   private final VkRenderingInfo renderingInfo;
//   private final DescriptorSetLayout textureLayout;
//   private final VulkanTextureSampler textureSampler;
//   private final DescriptorSetLayout geometryUniformLayout;

   private final VkClearValue clearColor;
   private final VkClearValue clearDepth;
   private VkRenderingAttachmentInfo.Buffer attachmentInfoColor;
   private VkRenderingAttachmentInfo attachmentInfoDepth;
   private MaterialAttachments materialAttachments;
   private Attachment ssaoColorAttachment;
   private VkRenderingAttachmentInfo.Buffer ssaoColorAttachmentInfo;
   private Attachment ssaoBlurAttachment;
   private VkRenderingAttachmentInfo.Buffer ssaoBlurAttachmentInfo;
   private VkAttachmentDescription.Buffer attachmentDescriptions;
   private VkSubpassDescription subpassDescription;
   private VkSubpassDependency.Buffer subpassDependencies;
   private long gBufferRenderPass;
   private long gBufferFrameBuffer;
   private long ssaoRenderPass;
   private long ssaoFrameBuffer;
   private long ssaoBlurRenderPass;
   private long ssaoBlurFrameBuffer;
   private VulkanTextureSampler textureSampler;
   private DescriptorSetLayout gBufferLayout;
   private DescriptorSetLayout ssaoLayout;
   private DescriptorSetLayout ssaoBlurLayout;
   private DescriptorSetLayout compositionLayout;


   // singletons
   private LogicalDevice device = DeviceManager.getDevice();
   private MemoryAllocationUtil allocationUtil = MemoryAllocationUtil.getInstance();
   private DescriptorAllocator allocator = DescriptorAllocator.getInstance();
   private PipelineCache pipelineCache = PipelineCache.getInstance();
   private MaterialCache materialCache = MaterialCache.getInstance();
   private TextureCache textureCache = TextureCache.getInstance();
   private VulkanScene scene = VulkanScene.getInstance();
   private GlobalBuffers globalBuffers = GlobalBuffers.getInstance();
   private VulkanSwapChain swapChain = VulkanSwapChain.getInstance();

   private AmbientOcclusionRenderer() {
      this.clearDepth = VkClearValue.calloc().color(c -> c.float32(0, 1f));
      this.clearColor = VkClearValue.calloc().color(c -> c.float32(0, 1f).float32(1, 1f));
      this.materialAttachments = new MaterialAttachments(device, allocationUtil, swapChain);
      this.attachmentInfoColor = initColorAttachmentInfo(materialAttachments, clearColor);
      this.attachmentInfoDepth = initDepthAttachmentInfo(materialAttachments, clearDepth);
      this.ssaoColorAttachment = initSSAOAttachment(device, allocationUtil, swapChain);
      this.ssaoColorAttachmentInfo = initSSAOColorAttachmentInfo(ssaoColorAttachment, clearColor);
      this.ssaoBlurAttachment = initSSAOAttachment(device, allocationUtil, swapChain);
      this.ssaoBlurAttachmentInfo = initSSAOColorAttachmentInfo(ssaoBlurAttachment, clearColor);
      this.gBufferRenderPass = createGBufferRenderPass(device, materialAttachments);
      this.gBufferFrameBuffer = createGBufferFrameBuffer(device, materialAttachments, gBufferRenderPass);
      this.ssaoRenderPass = createSSAORenderPass(device, ssaoColorAttachment);
      this.ssaoFrameBuffer = createSSAOFrameBuffer(device, ssaoColorAttachment, ssaoRenderPass);
      this.ssaoBlurRenderPass = createSSAOBlurRenderPass(device, ssaoBlurAttachment);
      this.ssaoBlurFrameBuffer = createSSAOBlurFrameBuffer(device, ssaoBlurAttachment, ssaoBlurRenderPass);
      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, VK13.VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE,
              1, VK13.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, true);

      this.gBufferLayout = initDescriptorSetLayout(device, VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 0,
              VK13.VK_SHADER_STAGE_VERTEX_BIT | VK13.VK_SHADER_STAGE_FRAGMENT_BIT);
      this.ssaoLayout = initDescriptorSetLayout(device, List.of(
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 2, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 3, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 4, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT)));
      this.ssaoBlurLayout = initDescriptorSetLayout(device, VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 0, VK13.VK_SHADER_STAGE_FRAGMENT_BIT);
      this.compositionLayout = initDescriptorSetLayout(device, List.of(
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 2, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 3, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 4, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT),
              new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 5, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT)));



//      depthAttachment = initDepthAttachment(device, allocationUtil);
//      this.depthAttachmentInfo = initDepthAttachmentInfo(depthAttachment, clearDepth);
//      this.colorAttachment = initColorAttachment(device, allocationUtil);
//      this.colorAttachmentInfo = initColorAttachmentInfo(colorAttachment, clearColor);
//      this.pushConstantBuffer = MemoryUtil.memAlloc(PUSH_CONSTANTS_SIZE);
//      this.renderingInfo = initRenderInfo(colorAttachmentInfo, depthAttachmentInfo);
      List<ShaderModule> shaderModules = initShaderModules(device);

//      this.geometryUniformLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
//              0, 1, VK13.VK_SHADER_STAGE_GEOMETRY_BIT));
//      long bufSize = (long)VulkanConstants.MAT4X4_SIZE * VulkanScene.SHADOW_MAP_CASCADE_COUNT;
//      this.projectionBuffers = VulkanUtils.createHostVisibleBuffers(device, allocationUtil, allocator, bufSize,
//              VulkanConstants.MAX_IN_FLIGHT, VK13.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, DESC_ID_PRJ, geometryUniformLayout);
//
//      this.fragmentStorageLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
//              0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
//
//      this.textureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT, VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK,
//              1, true);
//      this.textureLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
//              0, TextureCache.MAX_TEXTURES, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));
//
//      this.pipeline = initPipeline(device, pipelineCache, shaderModules,
//              List.of(geometryUniformLayout, textureLayout, fragmentStorageLayout));
//      shaderModules.forEach(s -> s.cleanup(device));
//
//      this.cascadeShadows = new ArrayList<>();
//      for(int i = 0; i < VulkanConstants.MAX_IN_FLIGHT; i++) {
//         cascadeShadows.add(new CascadeShadows());
//      }
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


   private static Attachment initSSAOAttachment(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanSwapChain swapChain) {
      return new Attachment(device, allocationUtil, swapChain.getSwapChainExtent().width(), swapChain.getSwapChainExtent().height(),
              VK13.VK_FORMAT_R8_UNORM, VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT, 1);
   }

   private static VkRenderingAttachmentInfo.Buffer initSSAOColorAttachmentInfo(Attachment sourceAttachment, VkClearValue clearValue) {
      return VkRenderingAttachmentInfo.calloc(1)
              .sType$Default()
              .imageView(sourceAttachment.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
              .clearValue(clearValue);
   }

   private static long createGBufferRenderPass(LogicalDevice device, MaterialAttachments attachments) {
      LongBuffer renderPassBuf = MemoryUtil.memAllocLong(1);
      try (var stack = MemoryStack.stackPush()) {
         var attachmentDescriptionBuffer = VkAttachmentDescription.calloc(attachments.getColorAttachments().size(), stack);
         for (int i = 0; i < attachments.getColorAttachments().size(); i++) {
            var colorAttachment = attachments.getColorAttachments().get(i);
            attachmentDescriptionBuffer.get(i)
                    .samples(VK13.VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK13.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .finalLayout(colorAttachment.isDepthAttachment() ?
                            VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL : VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .format(colorAttachment.getImage().getFormat());

            int colorAttachmentSize = attachments.getColorAttachments().size();
            var attachmentReferences = VkAttachmentReference.calloc(colorAttachmentSize, stack);
            for (int i = 0; i < colorAttachmentSize; i++) {
               attachmentReferences.get(i)
                       .attachment(i)
                       .layout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }
            var depthAttachmentReference = VkAttachmentReference.calloc(1, stack)
                    .get()
                    .attachment(3)
                    .layout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            var subpassDescriptions = VkSubpassDescription.calloc(1, stack);
            subpassDescriptions.get(0)
                    .pipelineBindPoint(VK13.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .pColorAttachments(attachmentReferences)
                    .colorAttachmentCount(attachments.getColorAttachments().size())
                    .pDepthStencilAttachment(depthAttachmentReference);

            var subpassDependencies = VkSubpassDependency.calloc(3, stack);
            subpassDependencies.get(0)
                    .srcSubpass(VK13.VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK13.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT)
                    .dstStageMask(VK13.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK13.VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT)
                    .srcAccessMask(VK13.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask(VK13.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK13.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT)
                    .dependencyFlags(0);

            subpassDependencies.get(1)
                    .srcSubpass(VK13.VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK13.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
                    .dstStageMask(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(VK13.VK_ACCESS_SHADER_READ_BIT)
                    .dstAccessMask(VK13.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dependencyFlags(VK13.VK_DEPENDENCY_BY_REGION_BIT);

            subpassDependencies.get(2)
                    .srcSubpass(0)
                    .dstSubpass(VK13.VK_SUBPASS_EXTERNAL)
                    .srcStageMask(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstStageMask(VK13.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT)
                    .srcAccessMask(VK13.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstAccessMask(VK13.VK_ACCESS_SHADER_READ_BIT)
                    .dependencyFlags(VK13.VK_DEPENDENCY_BY_REGION_BIT);

            var info = VkRenderPassCreateInfo.calloc()
                    .sType$Default()
                    .pAttachments(attachmentDescriptionBuffer)
                    .pSubpasses(subpassDescriptions)
                    .pDependencies(subpassDependencies);
            VulkanUtils.failIfNeeded(VK13.vkCreateRenderPass(device.getDevice(), info, null, renderPassBuf), "Error creating g-buffer render pass!");
         }
         return renderPassBuf.get(0);
      }
   }

   private static long createGBufferFrameBuffer(LogicalDevice device, MaterialAttachments attachments, long gBufferRenderPass) {
      LongBuffer attachmentBuffer = MemoryUtil.memAllocLong(attachments.getAllAttachments().size());
      for(int i = 0; i < attachments.getAllAttachments().size(); i++) {
         attachmentBuffer.put(i, attachments.getAllAttachments().get(i).getImageView().getImageViewId());
      }
      LongBuffer frameBuffer = MemoryUtil.memAllocLong(1);
      var info = VkFramebufferCreateInfo.calloc()
              .renderPass(gBufferRenderPass)
              .pAttachments(attachmentBuffer)
              .attachmentCount(attachments.getAllAttachments().size())
              .width(attachments.getWidth())
              .height(attachments.getHeight())
              .layers(1);
      VulkanUtils.failIfNeeded(VK13.vkCreateFramebuffer(device.getDevice(), info, null, frameBuffer), "Unable to create GBuffer Framebuffer!");
      return frameBuffer.get(0);
   }

   private static long createSSAORenderPass(LogicalDevice device, Attachment ssaoColorAttachment) {
      LongBuffer renderPass = MemoryUtil.memAllocLong(1);
      try(var stack = MemoryStack.stackPush()) {
         var attachmentDescription = VkAttachmentDescription.calloc(1, stack);
         attachmentDescription.get(0)
                 .format(ssaoColorAttachment.getImage().getFormat())
                 .samples(VK13.VK_SAMPLE_COUNT_1_BIT)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                 .stencilLoadOp(VK13.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                 .stencilStoreOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                 .initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED)
                 .finalLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
         var colorReference = VkAttachmentReference.calloc(1, stack);
         colorReference.get(0)
                 .layout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

         var subpassDescription = VkSubpassDescription.calloc(1, stack);
         subpassDescription.get(0)
                 .pipelineBindPoint(VK13.VK_PIPELINE_BIND_POINT_GRAPHICS)
                 .pColorAttachments(colorReference)
                 .colorAttachmentCount(1);

         var subpassDependencies = VkSubpassDependency.calloc(2, stack);
         subpassDependencies.get(0)
                 .srcSubpass(VK13.VK_SUBPASS_EXTERNAL)
                 .dstSubpass(0)
                 .srcStageMask(VK13.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                 .dstStageMask(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                 .srcAccessMask(VK13.VK_ACCESS_MEMORY_READ_BIT)
                 .dstAccessMask(VK13.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                 .dependencyFlags(VK13.VK_DEPENDENCY_BY_REGION_BIT);
         subpassDependencies.get(1)
                 .srcSubpass(0)
                 .dstSubpass(VK13.VK_SUBPASS_EXTERNAL)
                 .srcStageMask(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                 .dstStageMask(VK13.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                 .srcAccessMask(VK13.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                 .dstAccessMask(VK13.VK_ACCESS_MEMORY_READ_BIT)
                 .dependencyFlags(VK13.VK_DEPENDENCY_BY_REGION_BIT);

         var info = VkRenderPassCreateInfo.calloc()
                 .sType$Default()
                 .pAttachments(attachmentDescription)
                 .pSubpasses(subpassDescription)
                 .pDependencies(subpassDependencies);
         VulkanUtils.failIfNeeded(VK13.vkCreateRenderPass(device.getDevice(), info, null, renderPass), "Failed to create SSAO render pass!");
      }
      return renderPass.get(0);
   }

   private static long createSSAOFrameBuffer(LogicalDevice device, Attachment ssaoColorAttachment, long ssaoRenderPass) {
      LongBuffer colorAttachmentBuf = MemoryUtil.memAllocLong(1);
      colorAttachmentBuf.put(0, ssaoColorAttachment.getImageView().getImageViewId());
      LongBuffer frameBuffer = MemoryUtil.memAllocLong(1);
      var info = VkFramebufferCreateInfo.calloc()
              .renderPass(ssaoRenderPass)
              .pAttachments(colorAttachmentBuf)
              .attachmentCount(1)
              .width(ssaoColorAttachment.getImage().getWidth())
              .height(ssaoColorAttachment.getImage().getHeight())
              .layers(1);
      VulkanUtils.failIfNeeded(VK13.vkCreateFramebuffer(device.getDevice(), info, null, frameBuffer), "Unable to create SSAO Framebuffer!");
      return frameBuffer.get(0);
   }

   private static long createSSAOBlurRenderPass(LogicalDevice device, Attachment ssaoBlurAttachment) {
      LongBuffer renderPass = MemoryUtil.memAllocLong(1);
      try(var stack = MemoryStack.stackPush()) {
         var attachmentDescription = VkAttachmentDescription.calloc(1, stack);
         attachmentDescription.get(0)
                 .format(ssaoBlurAttachment.getImage().getFormat())
                 .samples(VK13.VK_SAMPLE_COUNT_1_BIT)
                 .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
                 .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE)
                 .stencilLoadOp(VK13.VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                 .stencilStoreOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                 .initialLayout(VK13.VK_IMAGE_LAYOUT_UNDEFINED)
                 .finalLayout(VK13.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
         var colorReference = VkAttachmentReference.calloc(1, stack);
         colorReference.get(0)
                 .layout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

         var subpassDescription = VkSubpassDescription.calloc(1, stack);
         subpassDescription.get(0)
                 .pipelineBindPoint(VK13.VK_PIPELINE_BIND_POINT_GRAPHICS)
                 .pColorAttachments(colorReference)
                 .colorAttachmentCount(1);

         var subpassDependencies = VkSubpassDependency.calloc(2, stack);
         subpassDependencies.get(0)
                 .srcSubpass(VK13.VK_SUBPASS_EXTERNAL)
                 .dstSubpass(0)
                 .srcStageMask(VK13.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                 .dstStageMask(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                 .srcAccessMask(VK13.VK_ACCESS_MEMORY_READ_BIT)
                 .dstAccessMask(VK13.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                 .dependencyFlags(VK13.VK_DEPENDENCY_BY_REGION_BIT);
         subpassDependencies.get(1)
                 .srcSubpass(0)
                 .dstSubpass(VK13.VK_SUBPASS_EXTERNAL)
                 .srcStageMask(VK13.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                 .dstStageMask(VK13.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                 .srcAccessMask(VK13.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK13.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                 .dstAccessMask(VK13.VK_ACCESS_MEMORY_READ_BIT)
                 .dependencyFlags(VK13.VK_DEPENDENCY_BY_REGION_BIT);

         var info = VkRenderPassCreateInfo.calloc()
                 .sType$Default()
                 .pAttachments(attachmentDescription)
                 .pSubpasses(subpassDescription)
                 .pDependencies(subpassDependencies);
         VulkanUtils.failIfNeeded(VK13.vkCreateRenderPass(device.getDevice(), info, null, renderPass), "Failed to create SSAOBlur render pass!");
      }
      return renderPass.get(0);
   }

   private static long createSSAOBlurFrameBuffer(LogicalDevice device, Attachment ssaoBlurAttachment, long ssaoBlurRenderPass) {
      LongBuffer colorAttachmentBuf = MemoryUtil.memAllocLong(1);
      colorAttachmentBuf.put(0, ssaoBlurAttachment.getImageView().getImageViewId());
      LongBuffer frameBuffer = MemoryUtil.memAllocLong(1);
      var info = VkFramebufferCreateInfo.calloc()
              .renderPass(ssaoBlurRenderPass)
              .pAttachments(colorAttachmentBuf)
              .attachmentCount(1)
              .width(ssaoBlurAttachment.getImage().getWidth())
              .height(ssaoBlurAttachment.getImage().getHeight())
              .layers(1);
      VulkanUtils.failIfNeeded(VK13.vkCreateFramebuffer(device.getDevice(), info, null, frameBuffer), "Unable to create SSAOBlur Framebuffer!");
      return frameBuffer.get(0);
   }

   private static DescriptorSetLayout initDescriptorSetLayout(LogicalDevice device, int type, int binding, int stage) {
      var info = new DescriptorSetLayoutInfo(type, binding, 1, stage);
      return new DescriptorSetLayout(device, info);
   }

   private static DescriptorSetLayout initDescriptorSetLayout(LogicalDevice device, List<DescriptorSetLayoutInfo> infos) {
      return new DescriptorSetLayout(device, infos);
   }

//   private static Attachment initDepthAttachment(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
//      return new Attachment(device, allocationUtil, ShadowUtils.SHADOW_MAP_SIZE, ShadowUtils.SHADOW_MAP_SIZE,
//              DEPTH_FORMAT, VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT, VulkanScene.SHADOW_MAP_CASCADE_COUNT);
//   }

//   private static VkRenderingAttachmentInfo initDepthAttachmentInfo(Attachment depthAttachment, VkClearValue clearValue) {
//      return VkRenderingAttachmentInfo.calloc()
//              .sType$Default()
//              .imageView(depthAttachment.getImageView().getImageViewId())
//              .imageLayout(VK13.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
//              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_CLEAR)
//              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_DONT_CARE)
//              .clearValue(clearValue);
//   }

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

//   private static VkRenderingInfo initRenderInfo(VkRenderingAttachmentInfo.Buffer colorAttachmentInfo,
//                                                 VkRenderingAttachmentInfo depthAttachments) {
//      var result = VkRenderingInfo.calloc().sType$Default();
//      try(var stack = MemoryStack.stackPush()) {
//         VkExtent2D extent = VkExtent2D.calloc(stack)
//                 .width(ShadowUtils.SHADOW_MAP_SIZE)
//                 .height(ShadowUtils.SHADOW_MAP_SIZE);
//         var renderArea = VkRect2D.calloc(stack).extent(extent);
//         result.renderArea(renderArea)
//                 .layerCount(VulkanScene.SHADOW_MAP_CASCADE_COUNT)
//                 .pColorAttachments(colorAttachmentInfo)
//                 .pDepthAttachment(depthAttachments);
//      }
//      return result;
//   }

   private static List<ShaderModule> initShaderModules(LogicalDevice device) {
      ShaderCompiler.compileShadersIfChanged(SHADERS);
      return SHADERS.stream().map(info -> info.getShaderModule(device, null)).toList();
   }

   @Override
   public void cleanup() {
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

   @Override
   public void load() {
      loadMaterials();
   }

   public CascadeShadows getCascadeShadows(int currentFrame) {
      return cascadeShadows.get(currentFrame);
   }

   public Attachment getColorAttachment() {
      return colorAttachment;
   }

   private void loadMaterials() {
      DescriptorSet set = allocator.addDescriptorSet(device, DESC_ID_MAT, fragmentStorageLayout);
      var layoutInfo = fragmentStorageLayout.getLayoutInfo();
      var buffer = materialCache.getMaterialsBuffer();
      set.setBuffer(device, buffer, buffer.getRequestedSize(), layoutInfo.binding(), layoutInfo.descriptorType());

      List<VulkanImageView> imageViews = textureCache.getAsList().stream().map(VulkanTexture::getImageView).toList();
      var textureSet = allocator.addDescriptorSet(device, DESC_ID_TEXT, textureLayout);
      textureSet.setImagesArray(device, imageViews, textureSampler, 0);
   }

   @Override
   public void render(CommandBuffer commandBuffer, int currentFrame, int imageIndex) {
      render(commandBuffer, currentFrame);
   }

   @Override
   public void resize() {

   }

   private void render(CommandBuffer cmdBuffer, int currentFrame) {
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

   public static AmbientOcclusionRenderer getInstance() {
      return new AmbientOcclusionRenderer();
   }
}
