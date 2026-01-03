package tv.memoryleakdeath.ascalondreams.animations;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.ComputeQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.ModelCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanAnimation;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMesh;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.ComputePipeline;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.PipelineCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationRenderer {
   private static final Logger logger = LoggerFactory.getLogger(AnimationRenderer.class);
   private static final String COMPUTE_SHADER_FILE_GLSL = "shaders/animation_compute_shader.glsl";
   private static final String COMPUTE_SHADER_FILE_SPV = COMPUTE_SHADER_FILE_GLSL + ".spv";
   private static final int LOCAL_SIZE_X = 32;
   private static final int PUSH_CONSTANTS_SIZE = VulkanConstants.PTR_SIZE * 5;

   private final CommandBuffer cmdBuffer;
   private final CommandPool cmdPool;
   private final ComputeQueue computeQueue;
   private final Fence fence;
   private final Map<String, Integer> groupSizeMap = new HashMap<>();
   private final ComputePipeline pipeline;
   private final ByteBuffer pushConstantsBuffer;
   private final DescriptorSetLayout stagingLayout;

   public AnimationRenderer(LogicalDevice device, PipelineCache pipelineCache) {
      this.fence = new Fence(device, true);
      this.computeQueue = new ComputeQueue(device, 0);
      this.cmdPool = new CommandPool(device, computeQueue.getQueueFamilyIndex(), false);
      this.cmdBuffer = new CommandBuffer(device, cmdPool, true, true);
      this.stagingLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_COMPUTE_BIT));

      ShaderModule module = initShaderModule(device);
      this.pushConstantsBuffer = MemoryUtil.memAlloc(PUSH_CONSTANTS_SIZE);
      this.pipeline = new ComputePipeline(device, pipelineCache, module, PUSH_CONSTANTS_SIZE,
              List.of(stagingLayout, stagingLayout, stagingLayout, stagingLayout));
      module.cleanup(device);
   }

   private static ShaderModule initShaderModule(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(COMPUTE_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_compute_shader, true);
      return new ShaderModule(device, VK13.VK_SHADER_STAGE_COMPUTE_BIT, COMPUTE_SHADER_FILE_SPV, null);
   }

   public void cleanup(LogicalDevice device) {
      MemoryUtil.memFree(pushConstantsBuffer);
      pipeline.cleanup(device);
      stagingLayout.cleanup(device);
      fence.cleanup(device);
      cmdBuffer.cleanup(device, cmdPool);
      cmdPool.cleanup(device);
   }

   public void loadModels(ModelCache modelCache) {
      var animatedModels = modelCache.getModelMap().values().stream().filter(VulkanModel::hasAnimations).toList();
      animatedModels.forEach(model ->{
         for(VulkanMesh mesh : model.getMeshList()) {
            int vertexSize = 14 * VulkanConstants.FLOAT_SIZE;
            int groupSize = (int) Math.ceil(((float) mesh.vertexBuffer().getRequestedSize() / vertexSize) / LOCAL_SIZE_X);
            groupSizeMap.put(mesh.id(), groupSize);
         }
      });
   }

   private void recordingStart(LogicalDevice device) {
      cmdPool.reset(device);
      cmdBuffer.beginRecording();
   }

   private void recordingStop() {
      cmdBuffer.endRecording();
   }

   public void render(LogicalDevice device, VulkanScene scene, ModelCache modelCache) {
      AnimationCache animationCache = AnimationCache.getInstance();
      fence.fenceWait(device);
      fence.reset(device);

      try(var stack = MemoryStack.stackPush()) {
         recordingStart(device);
         VulkanUtils.memoryBarrier(cmdBuffer, VK13.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT, VK13.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                 0, VK13.VK_ACCESS_SHADER_WRITE_BIT, 0);
         VkCommandBuffer cmdHandle = cmdBuffer.getCommandBuffer();
         VK13.vkCmdBindPipeline(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getId());

         var animatedEntities = scene.getEntities().stream().filter(e -> {
            VulkanModel model = modelCache.getModel(e.getModelId());
            return (e.getEntityAnimation() != null && model.hasAnimations());
         }).toList();

         animatedEntities.forEach(e -> {
            VulkanModel model = modelCache.getModel(e.getModelId());
            VulkanAnimation animation = model.getAnimationList().get(e.getEntityAnimation().getAnimationIndex());
            long jointsBufferAddress = animation.frameBuffers().get(e.getEntityAnimation().getCurrentFrame()).getAddress();
            model.getMeshList().forEach(mesh -> {
               setPushConstants(cmdHandle,
                       mesh.vertexBuffer().getAddress(),
                       mesh.weightsBuffer().getAddress(),
                       jointsBufferAddress,
                       animationCache.getBuffer(e.getId(), mesh.id()).getAddress(),
                       mesh.vertexBuffer().getRequestedSize() / VulkanConstants.FLOAT_SIZE);

               VK13.vkCmdDispatch(cmdHandle, groupSizeMap.get(mesh.id()), 1, 1);
            });
         });
         recordingStop();

         var commands = VkCommandBufferSubmitInfo.calloc(1, stack)
                 .sType$Default()
                 .commandBuffer(cmdBuffer.getCommandBuffer());
         computeQueue.submit(commands, null, null, fence);
      }
   }

   private void setPushConstants(VkCommandBuffer cmdHandle, long sourceBufferAddress, long weightsBufferAddress,
                                 long jointsBufferAddress, long destinationAddress, long sourceBufferFloatSize) {
      int offset = 0;
      pushConstantsBuffer.putLong(offset, sourceBufferAddress);
      offset += VulkanConstants.PTR_SIZE;
      pushConstantsBuffer.putLong(offset, weightsBufferAddress);
      offset += VulkanConstants.PTR_SIZE;
      pushConstantsBuffer.putLong(offset, jointsBufferAddress);
      offset += VulkanConstants.PTR_SIZE;
      pushConstantsBuffer.putLong(offset, destinationAddress);
      offset += VulkanConstants.PTR_SIZE;
      pushConstantsBuffer.putLong(offset, sourceBufferFloatSize);
      VK13.vkCmdPushConstants(cmdHandle, pipeline.getLayoutId(), VK13.VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstantsBuffer);
   }
}
