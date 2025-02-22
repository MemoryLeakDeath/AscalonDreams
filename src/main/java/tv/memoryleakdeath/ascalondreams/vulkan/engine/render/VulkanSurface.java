package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanSurface {
   private PhysicalDevice device;
   private long id;

   public VulkanSurface(PhysicalDevice device, long windowHandle) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         LongBuffer surfacePointer = stack.mallocLong(1);
         GLFWVulkan.glfwCreateWindowSurface(device.getPhysicalDevice().getInstance(), windowHandle, null, surfacePointer);
         this.id = surfacePointer.get(0);
      }
   }

   public VkSurfaceFormatKHR.Buffer getSurfaceFormats() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer buf = stack.mallocInt(1);
         VulkanUtils.failIfNeeded(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), id, buf, null), "Unable to retrieve count of surface formats!");
         int numFormats = buf.get(0);
         if (numFormats <= 0) {
            throw new RuntimeException("No surface formats found!");
         }

         VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.calloc(numFormats, stack);
         VulkanUtils.failIfNeeded(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), id, buf, surfaceFormats), "Unable to get surface formats!");
         return surfaceFormats;
      }
   }

   public void cleanup() {
      KHRSurface.vkDestroySurfaceKHR(device.getPhysicalDevice().getInstance(), id, null);
   }

   public long getId() {
      return id;
   }
}
