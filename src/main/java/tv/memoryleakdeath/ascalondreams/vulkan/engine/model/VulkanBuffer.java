package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanBuffer {
   private long allocationSize;
   private long buffer;
   private long vmaAllocation;
   private long memory;
   private PointerBuffer pointerBuffer;
   private long requestedSize;
   private long mappedMemory;
   private Long address;

   public VulkanBuffer(LogicalDevice device, MemoryAllocationUtil allocationUtil, long size, int bufferUsage, int vmaUsage, int vmaFlags, int requiredFlags) {
      this.requestedSize = size;
      this.mappedMemory = MemoryUtil.NULL;
      try(var stack = MemoryStack.stackPush()) {
         var bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                 .sType$Default()
                 .size(requestedSize)
                 .usage(bufferUsage)
                 .sharingMode(VK13.VK_SHARING_MODE_EXCLUSIVE);
         var vmaAllocInfo = VmaAllocationCreateInfo.calloc(stack)
                 .usage(vmaUsage)
                 .flags(vmaFlags)
                 .requiredFlags(requiredFlags);
         PointerBuffer allocBuf = stack.callocPointer(1);
         LongBuffer longBuf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(Vma.vmaCreateBuffer(allocationUtil.getVmaAllocator(), bufferCreateInfo, vmaAllocInfo, longBuf, allocBuf, null), "Failed to create buffer!");
         this.buffer = longBuf.get(0);
         this.vmaAllocation = allocBuf.get(0);
         this.pointerBuffer = MemoryUtil.memAllocPointer(1);
         if((bufferUsage & VK13.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT) > 0) {
            this.address = VulkanUtils.getBufferAddress(device, buffer);
         }
      }
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      MemoryUtil.memFree(pointerBuffer);
      unMap(device, allocationUtil);
      Vma.vmaDestroyBuffer(allocationUtil.getVmaAllocator(), buffer, vmaAllocation);
   }

   public void flush(MemoryAllocationUtil allocationUtil) {
      Vma.vmaFlushAllocation(allocationUtil.getVmaAllocator(), vmaAllocation, 0, VK13.VK_WHOLE_SIZE);
   }

   public long getAddress() {
      if(address == null) {
         throw new IllegalStateException("Buffer was not created with VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT flag!");
      }
      return address;
   }

   public long getBuffer() {
      return buffer;
   }

   public long getRequestedSize() {
      return requestedSize;
   }

   public long map(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      if(mappedMemory == MemoryUtil.NULL) {
         VulkanUtils.failIfNeeded(Vma.vmaMapMemory(allocationUtil.getVmaAllocator(), vmaAllocation, pointerBuffer), "Failed to map buffer!");
         mappedMemory = pointerBuffer.get(0);
      }
      return mappedMemory;
   }

   public void unMap(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      if(mappedMemory != MemoryUtil.NULL) {
         Vma.vmaUnmapMemory(allocationUtil.getVmaAllocator(), vmaAllocation);
         mappedMemory = MemoryUtil.NULL;
      }
   }
}
