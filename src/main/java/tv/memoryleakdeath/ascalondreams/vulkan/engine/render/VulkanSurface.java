package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;

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

   public void cleanup() {
      KHRSurface.vkDestroySurfaceKHR(device.getPhysicalDevice().getInstance(), id, null);
   }

   public long getId() {
      return id;
   }
}
