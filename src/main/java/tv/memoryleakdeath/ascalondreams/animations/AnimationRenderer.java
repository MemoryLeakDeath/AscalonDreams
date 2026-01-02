package tv.memoryleakdeath.ascalondreams.animations;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorAllocator;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSet;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayout;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.descriptor.DescriptorSetLayoutInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.CommandPool;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.ComputeQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Fence;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.ModelCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanAnimation;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanBuffer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanMesh;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.ComputePipeline;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.PipelineCache;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.Entity;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderCompiler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders.ShaderModule;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanConstants;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationRenderer {
   private static final Logger logger = LoggerFactory.getLogger(AnimationRenderer.class);
   private static final String COMPUTE_SHADER_FILE_GLSL = "shaders/animation_compute_shader.glsl";
   private static final String COMPUTE_SHADER_FILE_SPV = COMPUTE_SHADER_FILE_GLSL + ".spv";
   private static final int LOCAL_SIZE_X = 32;

   private final CommandBuffer cmdBuffer;
   private final CommandPool cmdPool;
   private final ComputeQueue computeQueue;
   private final Fence fence;
   private final Map<String, Integer> groupSizeMap = new HashMap<>();
   private final ComputePipeline pipeline;
   private final DescriptorSetLayout stagingLayout;

   public AnimationRenderer(LogicalDevice device, PipelineCache pipelineCache) {
      this.fence = new Fence(device, true);
      this.computeQueue = new ComputeQueue(device, 0);
      this.cmdPool = new CommandPool(device, computeQueue.getQueueFamilyIndex(), false);
      this.cmdBuffer = new CommandBuffer(device, cmdPool, true, true);
      this.stagingLayout = new DescriptorSetLayout(device, new DescriptorSetLayoutInfo(VK13.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER,
              0, 1, VK13.VK_SHADER_STAGE_COMPUTE_BIT));

      ShaderModule module = initShaderModule(device);
      this.pipeline = new ComputePipeline(device, pipelineCache, module, 0,
              List.of(stagingLayout, stagingLayout, stagingLayout, stagingLayout));
      module.cleanup(device);
   }

   private static ShaderModule initShaderModule(LogicalDevice device) {
      ShaderCompiler.compileShaderIfChanged(COMPUTE_SHADER_FILE_GLSL, Shaderc.shaderc_glsl_compute_shader, true);
      return new ShaderModule(device, VK13.VK_SHADER_STAGE_COMPUTE_BIT, COMPUTE_SHADER_FILE_SPV, null);
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      pipeline.cleanup(device);
      stagingLayout.cleanup(device);
      fence.cleanup(device);
      cmdBuffer.cleanup(device, cmdPool);
      cmdPool.cleanup(device);
   }

   public void loadModels(LogicalDevice device, DescriptorAllocator allocator, ModelCache modelCache, List<Entity> entities,
                          AnimationCache animationCache) {
      var animatedModels = modelCache.getModelMap().values().stream().filter(VulkanModel::hasAnimations).toList();
      animatedModels.forEach(model ->{
         String modelId = model.getId();
         int animationIndex = 0;
         for(VulkanAnimation animation : model.getAnimationList()) {
            int bufferPos = 0;
            for(VulkanBuffer jointsMatricesBuffer : animation.frameBuffers()) {
               String id = "%s_%d_%d".formatted(modelId, animationIndex, bufferPos);
               DescriptorSet descriptorSet = allocator.addDescriptorSet(device, id, stagingLayout);
               descriptorSet.setBuffer(device, jointsMatricesBuffer, jointsMatricesBuffer.getRequestedSize(), 0,
                       stagingLayout.getLayoutInfo().descriptorType());
               bufferPos++;
            }
            animationIndex++;
         }

         for(VulkanMesh mesh : model.getMeshList()) {
            int vertexSize = 14 * VulkanConstants.FLOAT_SIZE;
            int groupSize = (int) Math.ceil(((float) mesh.vertexBuffer().getRequestedSize() / vertexSize) / LOCAL_SIZE_X);
            DescriptorSet vertexDescriptorSet = allocator.addDescriptorSet(device, mesh.id() + "_VTX", stagingLayout);
            vertexDescriptorSet.setBuffer(device, mesh.vertexBuffer(), mesh.vertexBuffer().getRequestedSize(), 0, stagingLayout.getLayoutInfo().descriptorType());
            groupSizeMap.put(mesh.id(), groupSize);

            DescriptorSet weightsDescriptorSet = allocator.addDescriptorSet(device, mesh.id() + "_W", stagingLayout);
            weightsDescriptorSet.setBuffer(device, mesh.weightsBuffer(), mesh.weightsBuffer().getRequestedSize(), 0, stagingLayout.getLayoutInfo().descriptorType());
         }
      });

      var animatedEntities = entities.stream().filter(e -> {
         VulkanModel model = modelCache.getModel(e.getModelId());
         return model.hasAnimations();
      }).toList();

      animatedEntities.forEach(e -> {
         VulkanModel model = modelCache.getModel(e.getModelId());
         model.getMeshList().forEach(mesh -> {
            VulkanBuffer animationBuffer = animationCache.getBuffer(e.getId(), mesh.id());
            DescriptorSet descriptorSet = allocator.addDescriptorSet(device, "%s_%s_ENT".formatted(e.getId(), mesh.id()), stagingLayout);
            descriptorSet.setBuffer(device, animationBuffer, animationBuffer.getRequestedSize(), 0, stagingLayout.getLayoutInfo().descriptorType());
         });
      });
   }

   private void recordingStart(LogicalDevice device) {
      cmdPool.reset(device);
      cmdBuffer.beginRecording();
   }

   private void recordingStop() {
      cmdBuffer.endRecording();
   }

   public void render(LogicalDevice device, DescriptorAllocator allocator, VulkanScene scene, ModelCache modelCache) {
      fence.fenceWait(device);
      fence.reset(device);

      try(var stack = MemoryStack.stackPush()) {
         recordingStart(device);
         VulkanUtils.memoryBarrier(cmdBuffer, VK13.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT, VK13.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                 0, VK13.VK_ACCESS_SHADER_WRITE_BIT, 0);
         VkCommandBuffer cmdHandle = cmdBuffer.getCommandBuffer();
         VK13.vkCmdBindPipeline(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getId());

         LongBuffer descriptorSets = stack.mallocLong(4);
         var animatedEntities = scene.getEntities().stream().filter(e -> {
            VulkanModel model = modelCache.getModel(e.getModelId());
            return (e.getEntityAnimation() != null && model.hasAnimations());
         }).toList();

         animatedEntities.forEach(e -> {
            VulkanModel model = modelCache.getModel(e.getModelId());
            model.getMeshList().forEach(mesh -> {
               descriptorSets.put(0, allocator.getDescriptorSet(mesh.id() + "_VTX").getId());
               descriptorSets.put(1, allocator.getDescriptorSet(mesh.id() + "_W").getId());
               descriptorSets.put(2, allocator.getDescriptorSet(e.getId() + "_" + mesh.id() + "_ENT").getId());

               String id = "%s_%d_%d".formatted(e.getModelId(), e.getEntityAnimation().getAnimationIndex(), e.getEntityAnimation().getCurrentFrame());
               descriptorSets.put(3, allocator.getDescriptorSet(id).getId());

               VK13.vkCmdBindDescriptorSets(cmdHandle, VK13.VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getLayoutId(), 0, descriptorSets, null);
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
}
