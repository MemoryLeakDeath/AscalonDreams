package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.SecondaryCommandBufferInheritanceInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

public class CommandBuffer {
   private static final Logger logger = LoggerFactory.getLogger(CommandBuffer.class);
   private boolean oneTimeSubmit;
   private boolean primary;
   private VkCommandBuffer commandBuffer;

   public CommandBuffer(LogicalDevice device, CommandPool commandPool, boolean primary, boolean oneTimeSubmit) {
      logger.debug("Creating command buffer...");
      this.primary = primary;
      this.oneTimeSubmit = oneTimeSubmit;
      try(var stack = MemoryStack.stackPush()) {
         long commandBufferId = StructureUtils.createCommandBufferAllocateInfo(stack, device.getDevice(), commandPool.getId(), primary);
         this.commandBuffer = new VkCommandBuffer(commandBufferId, device.getDevice());
      }
   }

   public void beginRecording() {
      beginRecording(null);
   }
   public void beginRecording(SecondaryCommandBufferInheritanceInfo info) {
      try(var stack = MemoryStack.stackPush()) {
         var commandBufferInfo = VkCommandBufferBeginInfo.calloc(stack).sType$Default();
         if(oneTimeSubmit) {
            commandBufferInfo.flags(VK13.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
         }
         if(!primary) {
            if(info == null) {
               throw new RuntimeException("Secondary buffers must declare inheritance info");
            }
            commandBufferInfo.pInheritanceInfo(StructureUtils.createCommandBufferInheritanceInfo(stack, info));
         }
         VulkanUtils.failIfNeeded(VK13.vkBeginCommandBuffer(commandBuffer, commandBufferInfo), "Failed to begin command buffer");
      }
   }

   public void cleanup(LogicalDevice device, CommandPool pool) {
      logger.debug("Cleaning up command buffer");
      VK13.vkFreeCommandBuffers(device.getDevice(), pool.getId(), commandBuffer);
   }

   public void endRecording() {
      VulkanUtils.failIfNeeded(VK13.vkEndCommandBuffer(commandBuffer), "Failed to end command buffer!");
   }

   public void reset() {
      VK13.vkResetCommandBuffer(commandBuffer, VK13.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
   }

   public void submitAndWait(LogicalDevice device, BaseDeviceQueue queue) {
      Fence fence = new Fence(device, true);
      fence.reset(device);
      try(var stack = MemoryStack.stackPush()) {
         var commands = VkCommandBufferSubmitInfo.calloc(1, stack)
                 .sType$Default()
                 .commandBuffer(commandBuffer);
         queue.submit(commands, null, null, fence);
      }
   }

   public VkCommandBuffer getCommandBuffer() {
      return commandBuffer;
   }
}
