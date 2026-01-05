package tv.memoryleakdeath.ascalondreams.model;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCopy;
import tv.memoryleakdeath.ascalondreams.device.CommandBuffer;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.util.MemoryAllocationUtil;

public record TransferBuffer(VulkanBuffer sourceBuffer, VulkanBuffer destinationBuffer) {
   public void recordTransferCommand(CommandBuffer command) {
      try(var stack = MemoryStack.stackPush()) {
         VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                 .srcOffset(0)
                 .dstOffset(0)
                 .size(sourceBuffer.getRequestedSize());
         VK13.vkCmdCopyBuffer(command.getCommandBuffer(), sourceBuffer.getBuffer(), destinationBuffer.getBuffer(), copyRegion);
      }
   }

   public void cleanupSourceBuffer(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      sourceBuffer.cleanup(device, allocationUtil);
   }

   public void cleanupDestinationBuffer(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      destinationBuffer.cleanup(device, allocationUtil);
   }
}
