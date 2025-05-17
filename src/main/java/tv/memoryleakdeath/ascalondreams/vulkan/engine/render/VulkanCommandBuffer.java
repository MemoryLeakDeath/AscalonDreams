package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

public class VulkanCommandBuffer {
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
         VulkanUtils.failIfNeeded(VK14.vkBeginCommandBuffer(buffer, beginInfo), "Unable to create command buffer!");
      }
   }

   public void endRecording() {
      VulkanUtils.failIfNeeded(VK14.vkEndCommandBuffer(buffer), "Unable to end command buffer!");
   }

   public void reset() {
      VK14.vkResetCommandBuffer(buffer, VK14.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
   }

   public void cleanup() {
      VK14.vkFreeCommandBuffers(pool.getDevice().getDevice(), pool.getId(), buffer);
   }

   public VkCommandBuffer getBuffer() {
      return buffer;
   }

   public record BufferInheritance(long renderPassId, long frameBufferId, int subPass) {
   }
}
