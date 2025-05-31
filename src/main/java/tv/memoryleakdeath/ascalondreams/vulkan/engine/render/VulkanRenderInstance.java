package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.DebugHooksResults;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.DebugUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.ExtensionUtils;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.RequiredValidationLayerResults;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.StructureUtils;

public class VulkanRenderInstance {
   private static final Logger logger = LoggerFactory.getLogger(VulkanRenderInstance.class);

   private VkInstance vkInstance;

   private VkDebugUtilsMessengerCreateInfoEXT debugUtils;
   private long vulkanDebugHandle;

   public VulkanRenderInstance(boolean validation) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         // app info
         VkApplicationInfo appInfo = StructureUtils.createApplicationInfo(stack, "AscalonDreams", 1, 0);

         // set required validation layers
         RequiredValidationLayerResults requiredLayersResults = DebugUtils.getRequiredValidationLayers(stack, validation);

         // GLFW Extension
         PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
         if (glfwExtensions == null) {
            throw new RuntimeException("GLFW extensions were not found!");
         }
         boolean usePortabilityExt = ExtensionUtils.needsPortabilityExtensions();
         PointerBuffer requiredExtensions = ExtensionUtils.getRequiredExtensions(requiredLayersResults.supportsValidation(), usePortabilityExt, glfwExtensions, stack);
         DebugHooksResults debugHooksResults = DebugUtils.createDebugHooks(requiredLayersResults.supportsValidation());
         this.debugUtils = debugHooksResults.debugUtil();

         // create instance info
         this.vkInstance = StructureUtils.createInstance(stack, debugHooksResults.loggingExtension(),
                 appInfo, requiredLayersResults.requiredLayers(), requiredExtensions, usePortabilityExt);
         this.vulkanDebugHandle = DebugUtils.getDebugHandle(stack, requiredLayersResults.supportsValidation(),
                 vkInstance, debugUtils);
      } catch (Exception e) {
         logger.error("Unable to create vulkan render instance!", e);
      }
   }

   public void cleanup() {
      if (vulkanDebugHandle != VK14.VK_NULL_HANDLE) {
         EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(vkInstance, vulkanDebugHandle, null);
      }
      VK14.vkDestroyInstance(vkInstance, null);
      if (debugUtils != null) {
         debugUtils.pfnUserCallback().free();
         debugUtils.free();
      }
   }

   public VkInstance getVkInstance() {
      return vkInstance;
   }
}
