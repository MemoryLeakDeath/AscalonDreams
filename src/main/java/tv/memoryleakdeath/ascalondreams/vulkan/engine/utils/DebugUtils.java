package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkLayerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;

public final class DebugUtils {
   private static final Logger logger = LoggerFactory.getLogger(DebugUtils.class);
   private static final int MESSAGE_SEVERITY_MASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
           | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
   private static final int MESSAGE_TYPE_MASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
           | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
           | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
   public static final List<String> VALIDATION_FALLBACK_LAYERS = List.of("VK_LAYER_GOOGLE_threading",
           "VK_LAYER_LUNARG_parameter_validation",
           "VK_LAYER_LUNARG_object_tracker",
           "VK_LAYER_LUNARG_core_validation",
           "VK_LAYER_GOOGLE_unique_objects");

   private DebugUtils() {
   }

   public static RequiredValidationLayerResults getRequiredValidationLayers(MemoryStack stack, boolean enableValidation) {
      // validation layers
      List<String> validationLayers = getSupportedValidationLayers();
      int numValidationLayers = validationLayers.size();
      boolean supportsValidation = (enableValidation && numValidationLayers != 0);
      if (!supportsValidation) {
         logger.debug("No validation supported!");
      }

      // set required layers
      PointerBuffer requiredLayers = null;
      if (supportsValidation) {
         requiredLayers = getRequiredLayers(validationLayers, stack);
      }
      return new RequiredValidationLayerResults(requiredLayers, supportsValidation);
   }

   private static List<String> getSupportedValidationLayers() {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer numLayersArray = stack.callocInt(1);
         VK14.vkEnumerateInstanceLayerProperties(numLayersArray, null);
         int numLayers = numLayersArray.get(0);
         logger.debug("Number of supported validation layers: {}", numLayers);

         VkLayerProperties.Buffer props = VkLayerProperties.calloc(numLayers, stack);
         VK14.vkEnumerateInstanceLayerProperties(numLayersArray, props);
         List<String> supportedLayers = props.stream().map(VkLayerProperties::layerNameString).toList();
         if (logger.isDebugEnabled()) {
            logger.debug("Supported Layers: {}", supportedLayers);
         }

         return switch (supportedLayers) {
            case List<String> layers when layers.contains("VK_LAYER_KHRONOS_validation") -> {
               yield List.of("VK_LAYER_KHRONOS_validation");
            }
            case List<String> layers when layers.contains("VK_LAYER_LUNARG_standard_validation") -> {
               yield List.of("VK_LAYER_LUNARG_standard_validation");
            }
            default -> {
               yield VALIDATION_FALLBACK_LAYERS.stream().filter(supportedLayers::contains).toList();
            }
         };
      }
   }

   private static PointerBuffer getRequiredLayers(List<String> validationLayers, MemoryStack stack) {
      PointerBuffer requiredLayers = stack.mallocPointer(validationLayers.size());
      for (int i = 0; i < validationLayers.size(); i++) {
         requiredLayers.put(i, stack.ASCII(validationLayers.get(i)));
      }
      return requiredLayers;
   }

   public static DebugHooksResults createDebugHooks(boolean supportsValidation) {
      long loggingExtension = MemoryUtil.NULL;
      VkDebugUtilsMessengerCreateInfoEXT debugUtils = null;
      if (supportsValidation) {
         debugUtils = createDebugCallback();
         loggingExtension = debugUtils.address();
      }
      return new DebugHooksResults(debugUtils, loggingExtension);
   }

   private static VkDebugUtilsMessengerCreateInfoEXT createDebugCallback() {
      return VkDebugUtilsMessengerCreateInfoEXT.calloc()
              .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
              .messageSeverity(MESSAGE_SEVERITY_MASK)
              .messageType(MESSAGE_TYPE_MASK)
              .pfnUserCallback(((messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                 VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                 if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
                    logger.info("{}", callbackData.pMessageString());
                 } else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
                    logger.warn("{}", callbackData.pMessageString());
                 } else if ((messageSeverity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
                    logger.error("{}", callbackData.pMessageString());
                 } else {
                    logger.debug("{}", callbackData.pMessageString());
                 }
                 return VK14.VK_FALSE;
              }));
   }

   public static long getDebugHandle(MemoryStack stack, boolean supportsValidation,
                                     VkInstance vkInstance, VkDebugUtilsMessengerCreateInfoEXT debugUtils) {
      long vulkanDebugHandle = VK14.VK_NULL_HANDLE;
      if (supportsValidation) {
         LongBuffer longBuf = stack.mallocLong(1);
         VulkanUtils.failIfNeeded(EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vkInstance, debugUtils, null, longBuf), "Cannot create debug utils!");
         vulkanDebugHandle = longBuf.get(0);
      }
      return vulkanDebugHandle;
   }
}
