package tv.memoryleakdeath.ascalondreams.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.VK13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.device.DeviceManager;
import tv.memoryleakdeath.ascalondreams.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.render.VulkanRenderInstance;

public class MemoryAllocationUtil {
   private static final Logger logger = LoggerFactory.getLogger(MemoryAllocationUtil.class);
   private final long vmaAllocator;
   private static MemoryAllocationUtil memoryAllocationUtil;

   private MemoryAllocationUtil(VulkanRenderInstance instance) {
      LogicalDevice device = DeviceManager.getDevice();
      try(var stack = MemoryStack.stackPush()) {
         PointerBuffer allocBuf = stack.mallocPointer(1);
         var vmaVulkanFunctions = VmaVulkanFunctions.calloc(stack)
                 .set(instance.getVkInstance(), device.getDevice());
         var info = VmaAllocatorCreateInfo.calloc(stack)
                 .flags(Vma.VMA_ALLOCATOR_CREATE_BUFFER_DEVICE_ADDRESS_BIT)
                 .instance(instance.getVkInstance())
                 .vulkanApiVersion(VK13.VK_API_VERSION_1_3)
                 .device(device.getDevice())
                 .physicalDevice(device.getPhysicalDevice().getPhysicalDevice())
                 .pVulkanFunctions(vmaVulkanFunctions);
         VulkanUtils.failIfNeeded(Vma.vmaCreateAllocator(info, allocBuf), "Failed to create VMA Allocator");
         this.vmaAllocator = allocBuf.get(0);
      }
   }

   public static MemoryAllocationUtil createInstance(VulkanRenderInstance instance) {
      memoryAllocationUtil = new MemoryAllocationUtil(instance);
      return memoryAllocationUtil;
   }

   public static MemoryAllocationUtil getInstance() {
      return memoryAllocationUtil;
   }

   public void cleanup() {
      Vma.vmaDestroyAllocator(vmaAllocator);
   }

   public long getVmaAllocator() {
      return vmaAllocator;
   }
}
