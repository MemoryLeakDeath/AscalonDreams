package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.LongBuffer;

public class VulkanBuffer {
   private long allocationSize;
   private long id;
   private long memoryHandle;
   private long requestedSize;
   private long mappedMemoryHandle = MemoryUtil.NULL;
   private LogicalDevice device;
   private PointerBuffer pb;

   public VulkanBuffer(LogicalDevice device, long size, int usageType, int mask) {
      this.device = device;
      this.requestedSize = size;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                 .size(size)
                 .usage(usageType)
                 .sharingMode(VK14.VK_SHARING_MODE_EXCLUSIVE);
         LongBuffer lb = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateBuffer(device.getDevice(), bufferCreateInfo, null, lb), "Failed to create buffer structure!");
         this.id = lb.get(0);

         VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
         VK14.vkGetBufferMemoryRequirements(device.getDevice(), id, memoryRequirements);

         VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                 .allocationSize(memoryRequirements.size())
                 .memoryTypeIndex(device.getPhysicalDevice().getMemoryTypeFromProperties(memoryRequirements.memoryTypeBits(), mask));
         VulkanUtils.failIfNeeded(VK14.vkAllocateMemory(device.getDevice(), allocateInfo, null, lb), "Failed to allocate memory for buffer!");
         this.allocationSize = allocateInfo.allocationSize();
         this.memoryHandle = lb.get(0);
         this.pb = MemoryUtil.memAllocPointer(1);

         VulkanUtils.failIfNeeded(VK14.vkBindBufferMemory(device.getDevice(), id, memoryHandle, 0), "Failed to bind buffer memory!");
      }
   }

   public void cleanup() {
      MemoryUtil.memFree(pb);
      VK14.vkDestroyBuffer(device.getDevice(), id, null);
      VK14.vkFreeMemory(device.getDevice(), memoryHandle, null);
   }

   public long map() {
      if (mappedMemoryHandle == MemoryUtil.NULL) {
         VulkanUtils.failIfNeeded(VK14.vkMapMemory(device.getDevice(), memoryHandle, 0, allocationSize, 0, pb), "Failed to map buffer!");
         mappedMemoryHandle = pb.get(0);
      }
      return mappedMemoryHandle;
   }

   public void unmap() {
      if (mappedMemoryHandle != MemoryUtil.NULL) {
         VK14.vkUnmapMemory(device.getDevice(), memoryHandle);
         mappedMemoryHandle = MemoryUtil.NULL;
      }
   }

   public long getId() {
      return id;
   }

   public long getRequestedSize() {
      return requestedSize;
   }

   public PointerBuffer getBuffer() {
      return pb;
   }
}
