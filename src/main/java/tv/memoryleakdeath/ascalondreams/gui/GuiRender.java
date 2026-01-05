package tv.memoryleakdeath.ascalondreams.gui;

import imgui.ImGui;
import imgui.type.ImInt;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.cache.PipelineCache;
import tv.memoryleakdeath.ascalondreams.cache.TextureCache;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.device.VulkanGraphicsQueue;
import tv.memoryleakdeath.ascalondreams.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.model.VulkanTexture;
import tv.memoryleakdeath.ascalondreams.model.VulkanTextureSampler;
import tv.memoryleakdeath.ascalondreams.pojo.ImageSource;
import tv.memoryleakdeath.ascalondreams.pojo.PipelineBuildInfo;
import tv.memoryleakdeath.ascalondreams.pojo.PushConstantRange;
import tv.memoryleakdeath.ascalondreams.postprocess.PostProcessingRenderer;
import tv.memoryleakdeath.ascalondreams.render.Attachment;
import tv.memoryleakdeath.ascalondreams.render.Pipeline;
import tv.memoryleakdeath.ascalondreams.render.RenderChain;
import tv.memoryleakdeath.ascalondreams.render.Renderer;
import tv.memoryleakdeath.ascalondreams.render.VulkanImage;
import tv.memoryleakdeath.ascalondreams.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.util.StructureUtils;
import tv.memoryleakdeath.ascalondreams.util.VulkanConstants;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GuiRender implements Renderer {
   private static final Logger logger = LoggerFactory.getLogger(GuiRender.class);
   private static final String DESC_ID_TEXTURE = "GUI_DESC_ID_TEXT";
   private static final String GUI_FRAGMENT_SHADER_FILE_GLSL = "shaders/gui_fragment_shader.glsl";
   private static final String GUI_FRAGMENT_SHADER_FILE_SPV = GUI_FRAGMENT_SHADER_FILE_GLSL + ".spv";
   private static final String GUI_VERTEX_SHADER_FILE_GLSL = "shaders/gui_vertex_shader.glsl";
   private static final String GUI_VERTEX_SHADER_FILE_SPV = GUI_VERTEX_SHADER_FILE_GLSL + ".spv";

   private final List<VulkanBuffer> indexBuffers;
   private final List<VulkanBuffer> vertexBuffers;
   private final VulkanTexture fontTexture;
   private final VulkanTextureSampler fontTextureSampler;
   private final Map<Long, Long> guiTextureMap = new HashMap<>();
   private final Pipeline pipeline;
   private final DescriptorSetLayout textureDescriptorSetLayout;
   private VkRenderingAttachmentInfo.Buffer colorAttachmentInfo;
   private VkRenderingInfo renderingInfo;

   // singletons
   private LogicalDevice device = DeviceManager.getDevice();
   private DescriptorAllocator allocator = DescriptorAllocator.getInstance();
   private PipelineCache pipelineCache = PipelineCache.getInstance();
   private VulkanSwapChain swapChain = VulkanSwapChain.getInstance();
   private MemoryAllocationUtil allocationUtil = MemoryAllocationUtil.getInstance();
   private BaseDeviceQueue queue = VulkanGraphicsQueue.getInstance();
   private TextureCache cache = TextureCache.getInstance();

   private GuiRender(Attachment destinationAttachment) {
      this.indexBuffers = new ArrayList<>();
      this.vertexBuffers = new ArrayList<>();

      // adding dummy items to buffers to make logic easier later
      for(int i = 0; i < VulkanConstants.MAX_IN_FLIGHT; i++) {
         this.indexBuffers.add(null);
         this.vertexBuffers.add(null);
      }

      this.colorAttachmentInfo = initColorAttachmentInfo(destinationAttachment);
      this.renderingInfo = initRenderingInfo(destinationAttachment, colorAttachmentInfo);

      List<ShaderModule> shaderModules = initShaderModules(device);
      this.textureDescriptorSetLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
              0, 1, VK13.VK_SHADER_STAGE_FRAGMENT_BIT));

      this.pipeline = initPipeline(device, pipelineCache, shaderModules, List.of(textureDescriptorSetLayout));
      shaderModules.forEach(s -> s.cleanup(device));

      this.fontTexture = initUI(device, swapChain, allocationUtil, queue);
      this.fontTextureSampler = new VulkanTextureSampler(device, VK13.VK_SAMPLER_ADDRESS_MODE_REPEAT, VK13.VK_BORDER_COLOR_INT_OPAQUE_BLACK,
              1, true);

      DescriptorSet descriptorSet = allocator.addDescriptorSets(device, DESC_ID_TEXTURE, 1, textureDescriptorSetLayout).getFirst();
      descriptorSet.setImage(device, fontTexture.getImageView(), fontTextureSampler, textureDescriptorSetLayout.getLayoutInfo().binding());
   }

   private static VkRenderingAttachmentInfo.Buffer initColorAttachmentInfo(Attachment destinationAttachment) {
      return VkRenderingAttachmentInfo.calloc(1)
              .sType$Default()
              .imageView(destinationAttachment.getImageView().getImageViewId())
              .imageLayout(VK13.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
              .loadOp(VK13.VK_ATTACHMENT_LOAD_OP_LOAD)
              .storeOp(VK13.VK_ATTACHMENT_STORE_OP_STORE);
   }

   private static Pipeline initPipeline(LogicalDevice device, PipelineCache cache, List<ShaderModule> shaderModules, List<DescriptorSetLayout> layouts) {
      var vertexBufferStructure = new GuiVertexBufferStructure();
      var info = new PipelineBuildInfo(shaderModules, vertexBufferStructure.getVertexInputStateCreateInfo(),
              new int[] {PostProcessingRenderer.COLOR_FORMAT}, 0,
              List.of(new PushConstantRange(VK13.VK_SHADER_STAGE_VERTEX_BIT, 0, VulkanConstants.VEC2_SIZE)), layouts, true, false);
      var pipeline = new Pipeline(device, cache, info);
      vertexBufferStructure.cleanup();
      return pipeline;
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

   private static List<ShaderModule> initShaderModules(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(GUI_FRAGMENT_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_fragment_shader, true);
      ShaderCompiler.compileShaderIfChanged(GUI_VERTEX_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_vertex_shader, true);
      return List.of(new ShaderModule(device, VK13.VK_SHADER_STAGE_FRAGMENT_BIT, GUI_FRAGMENT_SHADER_FILE_SPV, null),
              new ShaderModule(device, VK13.VK_SHADER_STAGE_VERTEX_BIT, GUI_VERTEX_SHADER_FILE_SPV, null));
   }

   private static VulkanTexture initUI(LogicalDevice device, VulkanSwapChain swapChain, MemoryAllocationUtil allocationUtil, BaseDeviceQueue queue) {
      ImGui.createContext();

      var io = ImGui.getIO();
      io.setIniFilename(null);
      io.setDisplaySize(swapChain.getSwapChainExtent().width(), swapChain.getSwapChainExtent().height());
      io.setDisplayFramebufferScale(1f, 1f);

      var textureWidth = new ImInt();
      var textureHeight = new ImInt();
      ByteBuffer fontBuf = io.getFonts().getTexDataAsRGBA32(textureWidth, textureHeight);
      var imageSource = new ImageSource(fontBuf, textureWidth.get(), textureHeight.get(), 4);
      var fontsTexture = new VulkanTexture(device, allocationUtil, "GUI_TEXTURE", imageSource, VK13.VK_FORMAT_R8G8B8A8_SRGB, null);

      var commandPool = new CommandPool(device, queue.getQueueFamilyIndex(), false);
      var cmd = new CommandBuffer(device, commandPool, true, true);
      cmd.beginRecording();
      fontsTexture.recordTextureTransition(cmd);
      cmd.endRecording();
      cmd.submitAndWait(device, queue);
      cmd.cleanup(device, commandPool);
      commandPool.cleanup(device);

      return fontsTexture;
   }

   @Override
   public void cleanup() {
      fontTextureSampler.cleanup(device);
      fontTexture.cleanup(device, allocationUtil);
      textureDescriptorSetLayout.cleanup(device);
      pipeline.cleanup(device);
      vertexBuffers.stream().filter(Objects::nonNull).forEach(b -> b.cleanup(device, allocationUtil));
      indexBuffers.stream().filter(Objects::nonNull).forEach(b -> b.cleanup(device, allocationUtil));
      renderingInfo.free();
      colorAttachmentInfo.free();
   }

   @Override
   public void load() {
      loadTextures();
   }

   private void loadTextures() {
      List<GuiTexture> guiTextures = cache.getGuiTextures();
      if(guiTextures == null) {
         return;
      }
      for(GuiTexture texture : guiTextures) {
         DescriptorSet set = allocator.addDescriptorSets(device, texture.texturePath(), 1, textureDescriptorSetLayout).getFirst();
         VulkanTexture cachedTexture = cache.getTexture(texture.texturePath());
         set.setImage(device, cachedTexture.getImageView(), fontTextureSampler, textureDescriptorSetLayout.getLayoutInfo().binding());
         guiTextureMap.put(texture.id(), set.getId());
      }
   }

   @Override
   public void render(CommandBuffer commandBuffer, int currentFrame, int imageIndex) {
      PostProcessingRenderer postProcessingRenderer = RenderChain.getRendererInstance(PostProcessingRenderer.class);
      render(commandBuffer, currentFrame, postProcessingRenderer.getColorAttachment());
   }

   private void render(CommandBuffer commandBuffer, int currentFrame, Attachment destinationAttachment) {
      LogicalDevice device = DeviceManager.getDevice();
      DescriptorAllocator allocator = DescriptorAllocator.getInstance();
      MemoryAllocationUtil allocationUtil = MemoryAllocationUtil.getInstance();
      try(var stack = MemoryStack.stackPush()) {
         updateBuffers(device, allocationUtil, currentFrame);
         if(vertexBuffers.get(currentFrame) == null) {
            return;
         }

         VulkanImage destinationImage = destinationAttachment.getImage();
         int width = destinationImage.getWidth();
         int height = destinationImage.getHeight();
         var commandHandle = commandBuffer.getCommandBuffer();

         VK13.vkCmdBeginRendering(commandHandle, renderingInfo);
         VK13.vkCmdBindPipeline(commandHandle, VK13.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getId());
         StructureUtils.setupViewport(stack, width, height, commandHandle);

         LongBuffer vertexBuffer = stack.mallocLong(1);
         vertexBuffer.put(0, vertexBuffers.get(currentFrame).getBuffer());
         LongBuffer offsets = stack.mallocLong(1);
         offsets.put(0, 0L);
         VK13.vkCmdBindVertexBuffers(commandHandle, 0, vertexBuffer, offsets);
         VK13.vkCmdBindIndexBuffer(commandHandle, indexBuffers.get(currentFrame).getBuffer(), 0, VK13.VK_INDEX_TYPE_UINT16);
         GuiUtils.recordGuiRendering(stack, pipeline, allocator, commandHandle, DESC_ID_TEXTURE, guiTextureMap);
         VK13.vkCmdEndRendering(commandHandle);
      }
   }

   private void updateBuffers(LogicalDevice device, MemoryAllocationUtil allocationUtil, int index) {
      var imDrawData = ImGui.getDrawData();
      if(imDrawData.ptr == 0) {
         return;
      }

      int vertexBufferSize = imDrawData.getTotalVtxCount() * GuiVertexBufferStructure.VERTEX_SIZE;
      int indexBufferSize = imDrawData.getTotalIdxCount() * VulkanConstants.SHORT_SIZE;
      if(vertexBufferSize == 0 || indexBufferSize == 0) {
         return;
      }

      var vertexBuffer = vertexBuffers.get(index);
      if(vertexBuffer == null || vertexBufferSize > vertexBuffer.getRequestedSize()) {
         if(vertexBuffer != null) {
            vertexBuffer.cleanup(device, allocationUtil);
         }
         vertexBuffer = new VulkanBuffer(device, allocationUtil, vertexBufferSize, VK13.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                 Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT,
                 VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
         vertexBuffers.set(index, vertexBuffer);
      }

      var indexBuffer = indexBuffers.get(index);
      if(indexBuffer == null || indexBufferSize > indexBuffer.getRequestedSize()) {
         if(indexBuffer != null) {
            indexBuffer.cleanup(device, allocationUtil);
         }
         indexBuffer = new VulkanBuffer(device, allocationUtil, indexBufferSize, VK13.VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                 Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE, Vma.VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT,
                 VK13.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
         indexBuffers.set(index, indexBuffer);
      }

      ByteBuffer destinationVertexBuffer = MemoryUtil.memByteBuffer(vertexBuffer.map(device, allocationUtil), vertexBufferSize);
      ByteBuffer destinationIndexBuffer = MemoryUtil.memByteBuffer(indexBuffer.map(device, allocationUtil), indexBufferSize);
      int numCommandLists = imDrawData.getCmdListsCount();
      for(int i = 0; i < numCommandLists; i++) {
         ByteBuffer imguiVertexBuffer = imDrawData.getCmdListVtxBufferData(i);
         destinationVertexBuffer.put(imguiVertexBuffer);

         // always get the indices buffer after finishing with the vertices buffer
         ByteBuffer imguiIndexBuffer = imDrawData.getCmdListIdxBufferData(i);
         destinationIndexBuffer.put(imguiIndexBuffer);
      }

      vertexBuffer.flush(allocationUtil);
      indexBuffer.flush(allocationUtil);

      vertexBuffer.unMap(device, allocationUtil);
      indexBuffer.unMap(device, allocationUtil);
   }

   @Override
   public void resize() {
      PostProcessingRenderer postProcessingRenderer = RenderChain.getRendererInstance(PostProcessingRenderer.class);
      resize(postProcessingRenderer.getColorAttachment());
   }

   private void resize(Attachment destinationAttachment) {
      var io = ImGui.getIO();
      io.setDisplaySize(swapChain.getSwapChainExtent().width(), swapChain.getSwapChainExtent().height());

      renderingInfo.free();
      colorAttachmentInfo.free();
      colorAttachmentInfo = initColorAttachmentInfo(destinationAttachment);
      renderingInfo = initRenderingInfo(destinationAttachment, colorAttachmentInfo);
   }

   public static GuiRender getInstance() {
      PostProcessingRenderer postProcessingRenderer = RenderChain.getRendererInstance(PostProcessingRenderer.class);
      return new GuiRender(postProcessingRenderer.getColorAttachment());
   }
}
