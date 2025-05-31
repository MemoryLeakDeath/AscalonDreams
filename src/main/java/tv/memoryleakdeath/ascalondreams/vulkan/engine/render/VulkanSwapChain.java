package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanWindow;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.BaseDeviceQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.PhysicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.Semaphore;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.VulkanPresentationQueue;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.pojo.VulkanImageViewData;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VulkanSwapChain {
   public static final int TRIPLE_BUFFERING = 3;
   public static final int DOUBLE_BUFFERING = 2;

   private final LogicalDevice device;
   private int bufferingSetup = TRIPLE_BUFFERING;
   private SurfaceFormat surfaceFormats;
   private List<VulkanImageView> imageViews;
   private VkExtent2D swapChainExtent;
   private List<SyncSemaphores> semaphoreList = new ArrayList<>();
   private long id;
   private int currentFrame = 0;

   public VulkanSwapChain(LogicalDevice device, VulkanSurface surface, VulkanWindow window, int bufferingSetup, boolean vsync,
                          VulkanPresentationQueue presentationQueue, List<BaseDeviceQueue> concurrentQueues) {
      this.device = device;
      this.bufferingSetup = bufferingSetup;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         PhysicalDevice physicalDevice = device.getPhysicalDevice();
         VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
         VulkanUtils.failIfNeeded(
                 KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.getPhysicalDevice(), surface.getId(), surfaceCapabilities),
                 "Unable to retrieve surface capabilities!");
         int bestBufferingSetup = getBestBufferingSetup(surfaceCapabilities);
         getSurfaceFormat(physicalDevice, surface);
         getSwapChainExtent(window, surfaceCapabilities);

         this.id = StructureUtils.createSwapchainInfo(stack, surface.getId(), bestBufferingSetup,
                 surfaceFormats.imageFormat(), surfaceFormats.colorSpace(),
                 swapChainExtent, 1, surfaceCapabilities.currentTransform(),
                 true, vsync, concurrentQueues, presentationQueue, device.getDevice());
         buildImageViews(stack, device, id, surfaceFormats.imageFormat());
         imageViews.forEach(iv -> this.semaphoreList.add(new SyncSemaphores(device)));
      }
   }

   private int getBestBufferingSetup(VkSurfaceCapabilitiesKHR surfaceCapabilities) {
      int maxImages = surfaceCapabilities.maxImageCount();
      int minImages = surfaceCapabilities.minImageCount();
      int bestSetup = 0;
      if (maxImages != 0) {
         bestSetup = Math.min(bufferingSetup, maxImages);
      }
      bestSetup = Math.max(bestSetup, minImages);
      return bestSetup;
   }

   private void getSurfaceFormat(PhysicalDevice device, VulkanSurface surface) {
      VkSurfaceFormatKHR.Buffer surfaceFormats = surface.getSurfaceFormats();
      int imageFormat = VK14.VK_FORMAT_B8G8R8A8_SRGB;
      int colorSpace = surfaceFormats.get(0).colorSpace();
      this.surfaceFormats = surfaceFormats.stream()
              .filter(f -> f.format() == VK14.VK_FORMAT_B8G8R8A8_SRGB && f.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
              .findFirst()
              .map(f -> new SurfaceFormat(f.format(), f.colorSpace()))
              .orElse(new SurfaceFormat(imageFormat, colorSpace));
   }

   private void getSwapChainExtent(VulkanWindow window, VkSurfaceCapabilitiesKHR surfaceCapabilities) {
      VkExtent2D extent = VkExtent2D.calloc();
      if (surfaceCapabilities.currentExtent().width() == 0xFFFFFFFF) {
         // surface size undefined, set to window bounds
         int width = Math.min(window.getWidth(), surfaceCapabilities.maxImageExtent().width());
         width = Math.max(width, surfaceCapabilities.minImageExtent().width());

         int height = Math.min(window.getHeight(), surfaceCapabilities.maxImageExtent().height());
         height = Math.max(height, surfaceCapabilities.minImageExtent().height());

         extent.width(width);
         extent.height(height);
      } else {
         extent.set(surfaceCapabilities.currentExtent());
      }
      this.swapChainExtent = extent;
   }

   private void buildImageViews(MemoryStack stack, LogicalDevice device, long swapChain, int format) {
      IntBuffer buf = stack.mallocInt(1);
      VulkanUtils.failIfNeeded(KHRSwapchain.vkGetSwapchainImagesKHR(device.getDevice(), swapChain, buf, null), "Could not get count of swapchain images!");
      int numImages = buf.get(0);
      LongBuffer swapChainImages = stack.mallocLong(numImages);
      VulkanUtils.failIfNeeded(KHRSwapchain.vkGetSwapchainImagesKHR(device.getDevice(), swapChain, buf, swapChainImages), "Could not get swapchain images!");

      VulkanImageViewData viewData = new VulkanImageViewData();
      viewData.setFormat(format);
      viewData.setAspectMask(VK14.VK_IMAGE_ASPECT_COLOR_BIT);
      List<VulkanImageView> images = new ArrayList<>();
      for (int i = 0; i < numImages; i++) {
         images.add(new VulkanImageView(device, swapChainImages.get(i), viewData));
      }
      this.imageViews = Collections.unmodifiableList(images);
   }

   public void cleanup() {
      swapChainExtent.free();
      imageViews.forEach(VulkanImageView::cleanup);
      semaphoreList.forEach(SyncSemaphores::cleanup);
      KHRSwapchain.vkDestroySwapchainKHR(device.getDevice(), id, null);
   }

   public int aquireNextImage() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer iBuf = stack.mallocInt(1);
         int result = KHRSwapchain.vkAcquireNextImageKHR(device.getDevice(), id, Long.MAX_VALUE,
                 semaphoreList.get(currentFrame).imageAquisitionSemaphore().getId(), MemoryUtil.NULL, iBuf);
         return switch (result) {
            case KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR -> -1;
            case KHRSwapchain.VK_SUBOPTIMAL_KHR, VK14.VK_SUCCESS -> iBuf.get(0);
            default -> throw new RuntimeException("failed to aquire image, result: %d".formatted(result));
         };
      }
   }

   public boolean showImage(BaseDeviceQueue queue, int imageIndex) {
      boolean resize = false;
      try (MemoryStack stack = MemoryStack.stackPush()) {
         long[] semaphoreIds = new long[]{semaphoreList.get(currentFrame).renderCompleteSemaphore().getId()};
         VkPresentInfoKHR presentInfoKHR = StructureUtils.createPresentInfo(stack, 1,
                 new long[]{id}, new int[]{imageIndex}, semaphoreIds);
         int result = KHRSwapchain.vkQueuePresentKHR(queue.getQueue(), presentInfoKHR);
         if (result == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
            resize = true;
         } else if (result != VK14.VK_SUCCESS) {
            throw new RuntimeException("Failed to show KHR! result: %d".formatted(result));
         }
      }
      currentFrame = (currentFrame + 1) % imageViews.size();
      return resize;
   }

   public LogicalDevice getDevice() {
      return device;
   }

   public int getNumImages() {
      return imageViews.size();
   }

   public SurfaceFormat getSurfaceFormats() {
      return surfaceFormats;
   }

   public List<VulkanImageView> getImageViews() {
      return imageViews;
   }

   public int getWidth() {
      return swapChainExtent.width();
   }

   public int getHeight() {
      return swapChainExtent.height();
   }

   public int getCurrentFrame() {
      return currentFrame;
   }

   public List<SyncSemaphores> getSemaphoreList() {
      return semaphoreList;
   }

   public VkExtent2D getExtent() {
      return this.swapChainExtent;
   }

   public record SurfaceFormat(int imageFormat, int colorSpace) {
   }

   public record SyncSemaphores(Semaphore imageAquisitionSemaphore, Semaphore renderCompleteSemaphore) {
      public SyncSemaphores(LogicalDevice device) {
         this(new Semaphore(device), new Semaphore(device));
      }

      public void cleanup() {
         imageAquisitionSemaphore.cleanup();
         renderCompleteSemaphore.cleanup();
      }
   }
}
