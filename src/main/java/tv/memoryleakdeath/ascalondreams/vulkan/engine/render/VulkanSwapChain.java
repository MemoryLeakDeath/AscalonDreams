package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Semaphore;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.SurfaceFormat;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VulkanSwapChain {
   private static final Logger logger = LoggerFactory.getLogger(VulkanSwapChain.class);
   private final LogicalDevice device;
   private List<VulkanImageView> imageViews;
   private VkExtent2D swapChainExtent;
   private long id;

   public VulkanSwapChain(LogicalDevice device, VulkanSurface surface, VulkanWindow window, int bufferingSetup, boolean vsync) {
      this.device = device;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         int actualBufferingSetup = surface.calculateNumImages(bufferingSetup);
         this.swapChainExtent = surface.getLargestExtents(window);

         SurfaceFormat surfaceFormat = surface.getSurfaceFormat();
         var swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                 .sType$Default()
                 .surface(surface.getId())
                 .minImageCount(actualBufferingSetup)
                 .imageFormat(surfaceFormat.imageFormat())
                 .imageColorSpace(surfaceFormat.colorSpace())
                 .imageExtent(swapChainExtent)
                 .imageArrayLayers(1)
                 .imageUsage(VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                 .preTransform(surface.getSurfaceCapabilities().currentTransform())
                 .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                 .clipped(true);
         if (vsync) {
            swapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR);
         } else {
            swapchainCreateInfo.presentMode(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR);
         }
         LongBuffer buf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(KHRSwapchain.vkCreateSwapchainKHR(device.getDevice(), swapchainCreateInfo, null, buf), "Cannot create swapchain!");
         this.id = buf.get(0);
         buildImageViews(stack, device, id, surfaceFormat.imageFormat());
      }
   }

   private void buildImageViews(MemoryStack stack, LogicalDevice device, long swapChain, int format) {
      IntBuffer buf = stack.mallocInt(1);
      VulkanUtils.failIfNeeded(KHRSwapchain.vkGetSwapchainImagesKHR(device.getDevice(), swapChain, buf, null), "Could not get count of swapchain images!");
      int numImages = buf.get(0);

      LongBuffer swapChainImages = stack.mallocLong(numImages);
      VulkanUtils.failIfNeeded(KHRSwapchain.vkGetSwapchainImagesKHR(device.getDevice(), swapChain, buf, swapChainImages), "Could not get swapchain images!");

      VulkanImageViewData viewData = new VulkanImageViewData();
      viewData.setFormat(format);
      viewData.setAspectMask(VK13.VK_IMAGE_ASPECT_COLOR_BIT);
      List<VulkanImageView> images = new ArrayList<>();
      for (int i = 0; i < numImages; i++) {
         images.add(new VulkanImageView(device, swapChainImages.get(i), viewData, false));
      }
      this.imageViews = Collections.unmodifiableList(images);
   }

   public int acquireNextImage(LogicalDevice device, Semaphore imageAquireSemaphore) {
      try(var stack = MemoryStack.stackPush()) {
         IntBuffer buf = stack.mallocInt(1);
         int resultCode = KHRSwapchain.vkAcquireNextImageKHR(device.getDevice(), id, ~0L, imageAquireSemaphore.getId(), MemoryUtil.NULL, buf);
         if(resultCode == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
            return -1;
         } else if(resultCode == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            // not optimal but can still be used
         } else if(resultCode != VK13.VK_SUCCESS) {
            throw new RuntimeException("Failed to acquire image: %d".formatted(resultCode));
         }
         return buf.get(0);
      }
   }

   public VulkanImageView getImageView(int position) {
      return imageViews.get(position);
   }

   public boolean presentImage(BaseDeviceQueue queue, Semaphore renderCompleteSemaphore, int imageIndex) {
      boolean resize = false;
      try(var stack = MemoryStack.stackPush()) {
         var info = VkPresentInfoKHR.calloc(stack)
                 .sType$Default()
                 .pWaitSemaphores(stack.longs(renderCompleteSemaphore.getId()))
                 .swapchainCount(1)
                 .pSwapchains(stack.longs(id))
                 .pImageIndices(stack.ints(imageIndex));
         int resultCode = KHRSwapchain.vkQueuePresentKHR(queue.getQueue(), info);
         if(resultCode == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
            resize = true;
         } else if(resultCode == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            // not optimal but can be used
         } else if(resultCode != VK13.VK_SUCCESS) {
            throw new RuntimeException("Failed to present KHR: %d".formatted(resultCode));
         }
      }
      return resize;
   }

   public void cleanup() {
      logger.debug("Cleaning up vulkan swapchain");
      swapChainExtent.free();
      imageViews.forEach(VulkanImageView::cleanup);
      KHRSwapchain.vkDestroySwapchainKHR(device.getDevice(), id, null);
   }

   public LogicalDevice getDevice() {
      return device;
   }

   public int getNumImages() {
      return imageViews.size();
   }

   public List<VulkanImageView> getImageViews() {
      return imageViews;
   }

   public VkExtent2D getSwapChainExtent() {
      return swapChainExtent;
   }

   public long getId() {
      return id;
   }
}
