package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanBuffer {
   private long allocationSize;
   private long buffer;
   private long memory;
   private PointerBuffer pointerBuffer;
   private long requestedSize;
   private long mappedMemory;

   public VulkanBuffer(LogicalDevice device, long size, int usage, int mask) {
      this.requestedSize = size;
      this.mappedMemory = MemoryUtil.NULL;
      try(var stack = MemoryStack.stackPush()) {
         var bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                 .sType$Default()
                 .size(requestedSize)
                 .usage(usage)
                 .sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
         LongBuffer longBuf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK13.vkCreateBuffer(device.getDevice(), bufferCreateInfo, null, longBuf), "Failed to create buffer!");
         this.buffer = longBuf.get(0);

         var memoryRequirements = VkMemoryRequirements.calloc(stack);
         VK13.vkGetBufferMemoryRequirements(device.getDevice(), buffer, memoryRequirements);

         var memoryAllocation = VkMemoryAllocateInfo.calloc(stack)
                 .sType$Default()
                 .allocationSize(memoryRequirements.size())
                 .memoryTypeIndex(VulkanUtils.getMemoryType(device, memoryRequirements.memoryTypeBits(), mask));
         VulkanUtils.failIfNeeded(VK13.vkAllocateMemory(device.getDevice(), memoryAllocation, null, longBuf), "Failed to allocate memory for buffer!");
         this.allocationSize = memoryAllocation.allocationSize();
         this.memory = longBuf.get(0);
         this.pointerBuffer = MemoryUtil.memAllocPointer(1);

         VulkanUtils.failIfNeeded(VK13.vkBindBufferMemory(device.getDevice(), buffer, memory, 0), "Failed to bind buffer memory!");
      }
   }

   public void cleanup(LogicalDevice device) {
      MemoryUtil.memFree(pointerBuffer);
      VK13.vkDestroyBuffer(device.getDevice(), buffer, null);
      VK13.vkFreeMemory(device.getDevice(), memory, null);
   }

   public long getBuffer() {
      return buffer;
   }

   public long getRequestedSize() {
      return requestedSize;
   }

   public long map(LogicalDevice device) {
      if(mappedMemory == MemoryUtil.NULL) {
         VulkanUtils.failIfNeeded(VK13.vkMapMemory(device.getDevice(), memory, 0, allocationSize, 0, pointerBuffer), "Failed to map buffer!");
         mappedMemory = pointerBuffer.get(0);
      }
      return mappedMemory;
   }

   public void unMap(LogicalDevice device) {
      if(mappedMemory != MemoryUtil.NULL) {
         VK13.vkUnmapMemory(device.getDevice(), memory);
         mappedMemory = MemoryUtil.NULL;
      }
   }
}
