package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

public class VulkanCommandBuffer {
   private static final Logger logger = LoggerFactory.getLogger(VulkanCommandBuffer.class);
   private final VulkanCommandPool pool;
   private final boolean oneTimeSubmit;
   private final VkCommandBuffer buffer;
   private final boolean primary;

   public VulkanCommandBuffer(VulkanCommandPool pool, boolean primary, boolean oneTimeSubmit) {
      this.pool = pool;
      this.primary = primary;
      this.oneTimeSubmit = oneTimeSubmit;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         PointerBuffer buf = StructureUtils.createCommandBufferAllocateInfo(stack, pool.getDevice().getDevice(), pool.getId(), primary, 1);
         this.buffer = new VkCommandBuffer(buf.get(0), pool.getDevice().getDevice());
         logger.debug("init command buffer with address: {}", buffer.address());
      }
   }

   public void beginRecording() {
      beginRecording(null);
   }

   public void beginRecording(BufferInheritance inheritance) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
         if (oneTimeSubmit) {
            beginInfo.flags(VK14.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
         }
         if (!primary) {
            beginInfo.pInheritanceInfo(StructureUtils.buildInheritanceInfo(stack, inheritance));
            beginInfo.flags(VK14.VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT);
         }
         logger.debug("begin command buffer with address: {}", buffer.address());
         VulkanUtils.failIfNeeded(VK14.vkBeginCommandBuffer(buffer, beginInfo), "Unable to create command buffer!");
      }
   }

   public void endRecording() {
      logger.debug("end command buffer with address: {}", buffer.address());
      VulkanUtils.failIfNeeded(VK14.vkEndCommandBuffer(buffer), "Unable to end command buffer!");
   }

   public void reset() {
      logger.debug("reset command buffer with address: {}", buffer.address());
      VK14.vkResetCommandBuffer(buffer, VK14.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
   }

   public void cleanup() {
      logger.debug("cleaning command buffer with address: {}", buffer.address());
      VK14.vkFreeCommandBuffers(pool.getDevice().getDevice(), pool.getId(), buffer);
   }

   public VkCommandBuffer getBuffer() {
      return buffer;
   }

   public record BufferInheritance(long renderPassId, long frameBufferId, int subPass) {
   }
}
