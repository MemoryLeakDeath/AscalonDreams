package tv.memoryleakdeath.ascalondreams.vulkan.engine.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkMemoryRequirements;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.AllocateInfoResults;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

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
         this.id = StructureUtils.createBufferInfo(stack, device.getDevice(), size, usageType, VK14.VK_SHARING_MODE_EXCLUSIVE);

         VkMemoryRequirements memoryRequirements = VkMemoryRequirements.malloc(stack);
         VK14.vkGetBufferMemoryRequirements(device.getDevice(), id, memoryRequirements);

         AllocateInfoResults allocateInfoResults = StructureUtils.createMemoryAllocateInfo(stack, device.getDevice(), memoryRequirements.size(), device.getPhysicalDevice().getMemoryTypeFromProperties(memoryRequirements.memoryTypeBits(), mask));
         this.allocationSize = allocateInfoResults.size();
         this.memoryHandle = allocateInfoResults.handle();
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
