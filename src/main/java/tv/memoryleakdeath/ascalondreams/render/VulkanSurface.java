package tv.memoryleakdeath.ascalondreams.render;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.pojo.SurfaceFormat;
import tv.memoryleakdeath.ascalondreams.util.VulkanUtils;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanSurface {
   private static final Logger logger = LoggerFactory.getLogger(VulkanSurface.class);
   private PhysicalDevice device;
   private long id;
   private VkSurfaceCapabilitiesKHR surfaceCapabilities;
   private SurfaceFormat surfaceFormat;
   private static VulkanSurface vulkanSurface;

   private VulkanSurface(PhysicalDevice device, long windowHandle) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         LongBuffer surfacePointer = stack.mallocLong(1);
         GLFWVulkan.glfwCreateWindowSurface(device.getPhysicalDevice().getInstance(), windowHandle, null, surfacePointer);
         this.id = surfacePointer.get(0);

         this.surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc();
         VulkanUtils.failIfNeeded(KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getPhysicalDevice(), id, surfaceCapabilities), "Failed to get surface capabilities!");
         this.surfaceFormat = initSurfaceFormat();
      }
   }

   public static VulkanSurface createInstance(PhysicalDevice device, long windowHandle) {
      vulkanSurface = new VulkanSurface(device, windowHandle);
      return vulkanSurface;
   }

   public static VulkanSurface getInstance() {
      return vulkanSurface;
   }

   private SurfaceFormat initSurfaceFormat() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer buf = stack.mallocInt(1);
         VulkanUtils.failIfNeeded(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), id, buf, null), "Unable to retrieve count of surface formats!");
         int numFormats = buf.get(0);
         if (numFormats <= 0) {
            throw new RuntimeException("No surface formats found!");
         }

         VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.calloc(numFormats, stack);
         VulkanUtils.failIfNeeded(KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), id, buf, surfaceFormats), "Unable to get surface formats!");
         int imageFormat = VK13.VK_FORMAT_B8G8R8A8_UNORM;
         int colorSpace = surfaceFormats.get(0).colorSpace();
         return surfaceFormats.stream()
                 .filter(f -> f.format() == VK13.VK_FORMAT_B8G8R8A8_UNORM && f.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                 .findFirst()
                 .map(f -> new SurfaceFormat(f.format(), f.colorSpace()))
                 .orElse(new SurfaceFormat(imageFormat, colorSpace));
      }
   }


   public void cleanup() {
      logger.debug("Cleanup vulkan surface");
      surfaceCapabilities.free();
      KHRSurface.vkDestroySurfaceKHR(device.getPhysicalDevice().getInstance(), id, null);
   }

   public long getId() {
      return id;
   }

   public VkSurfaceCapabilitiesKHR getSurfaceCapabilities() {
      return surfaceCapabilities;
   }

   public SurfaceFormat getSurfaceFormat() {
      return surfaceFormat;
   }

   public int calculateNumImages(int requestedNumImages) {
      int maxImages = surfaceCapabilities.maxImageCount();
      int minImages = surfaceCapabilities.minImageCount();
      int result = minImages;
      if(maxImages != 0) {
         result = Math.min(requestedNumImages, maxImages);
      }
      result = Math.min(result, minImages);
      logger.debug("Requested {} images, got {} images -- max: {} min: {}", requestedNumImages, result, maxImages, minImages);
      return result;
   }

   public VkExtent2D getLargestExtents(VulkanWindow window) {
      var result = VkExtent2D.calloc();
      if(surfaceCapabilities.currentExtent().width() == 0xFFFFFFFF) {
         // surface size undefined. Set to window size if within bounds
         int width = Math.min(window.getWidth(), surfaceCapabilities.maxImageExtent().width());
         width = Math.max(width, surfaceCapabilities.minImageExtent().width());

         int height = Math.min(window.getHeight(), surfaceCapabilities.maxImageExtent().height());
         height = Math.max(height, surfaceCapabilities.minImageExtent().height());

         result.width(width);
         result.height(height);
      } else {
         // surface already defined, use those extents
         result.set(surfaceCapabilities.currentExtent());
      }
      return result;
   }
}
