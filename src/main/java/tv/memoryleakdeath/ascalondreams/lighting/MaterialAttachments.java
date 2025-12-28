package tv.memoryleakdeath.ascalondreams.lighting;

import org.lwjgl.vulkan.VK13;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.device.LogicalDevice;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.Attachment;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanSwapChain;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.MemoryAllocationUtil;

import java.util.ArrayList;
import java.util.List;

public class MaterialAttachments {
   public static final int ALBEDO_FORMAT = VK13.VK_FORMAT_R16G16B16A16_SFLOAT;
   public static final int DEPTH_FORMAT = VK13.VK_FORMAT_D16_UNORM;
   private final List<Attachment> colorAttachments = new ArrayList<>();
   private final Attachment depthAttachment;
   private final int width;
   private final int height;

   public MaterialAttachments(LogicalDevice device, MemoryAllocationUtil allocationUtil, VulkanSwapChain swapChain) {
      this.width = swapChain.getSwapChainExtent().width();
      this.height = swapChain.getSwapChainExtent().height();

      // albedo attachment
      colorAttachments.add(new Attachment(device, allocationUtil, width, height, ALBEDO_FORMAT, VK13.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT));

      // depth attachment
      depthAttachment = new Attachment(device, allocationUtil, width, height, DEPTH_FORMAT, VK13.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
   }

   public void cleanup(LogicalDevice device, MemoryAllocationUtil allocationUtil) {
      colorAttachments.forEach(a -> a.cleanup(device, allocationUtil));
      depthAttachment.cleanup(device, allocationUtil);
   }

   public List<Attachment> getAllAttachments() {
      List<Attachment> result = new ArrayList<>(colorAttachments);
      result.add(depthAttachment);
      return result;
   }

   public List<Attachment> getColorAttachments() {
      return colorAttachments;
   }

   public Attachment getDepthAttachment() {
      return depthAttachment;
   }

   public int getWidth() {
      return width;
   }

   public int getHeight() {
      return height;
   }
}
