package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.apache.commons.exec.OS;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkLayerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ExtensionUtils {
   private static final Logger logger = LoggerFactory.getLogger(ExtensionUtils.class);
   private static final String PORTABILITY_EXTENSION = "VK_KHR_portability_enumeration";

   private ExtensionUtils() {
   }

   public static boolean needsPortabilityExtensions() {
      Set<String> extensions = new HashSet<>();
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer numExtensionsArray = stack.callocInt(1);
         VK14.vkEnumerateInstanceExtensionProperties((String) null, numExtensionsArray, null);
         int numExtensions = numExtensionsArray.get(0);
         logger.debug("Number of supported extensions: {}", numExtensions);

         VkExtensionProperties.Buffer props = VkExtensionProperties.calloc(numExtensions, stack);
         VK14.vkEnumerateInstanceExtensionProperties((String) null, numExtensionsArray, props);
         extensions = props.stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toUnmodifiableSet());
         if (logger.isDebugEnabled()) {
            logger.debug("Supported Extensions: {}", extensions);
         }
      }
      return (extensions.contains(PORTABILITY_EXTENSION) && OS.isFamilyMac());
   }

   public static PointerBuffer getRequiredExtensions(boolean supportsValidation, boolean usePortabilityExt, PointerBuffer glfwExtensions, MemoryStack stack) {
      PointerBuffer requiredExtensions = null;
      if (supportsValidation) {
         ByteBuffer debugUtilsExt = stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
         int numExtensions = (usePortabilityExt ? glfwExtensions.remaining() + 2 : glfwExtensions.remaining() + 1);
         requiredExtensions = stack.mallocPointer(numExtensions);
         requiredExtensions.put(glfwExtensions).put(debugUtilsExt);
         if (usePortabilityExt) {
            requiredExtensions.put(stack.UTF8(PORTABILITY_EXTENSION));
         }
      } else {
         int numExtensions = (usePortabilityExt ? glfwExtensions.remaining() + 1 : glfwExtensions.remaining());
         requiredExtensions = stack.mallocPointer(numExtensions);
         requiredExtensions.put(glfwExtensions);
         if (usePortabilityExt) {
            requiredExtensions.put(stack.UTF8(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME));
         }
      }
      requiredExtensions.flip();
      return requiredExtensions;
   }

   private List<String> getSupportedValidationLayers() {
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
               yield DebugUtils.VALIDATION_FALLBACK_LAYERS.stream().filter(supportedLayers::contains).toList();
            }
         };
      }
   }

   private PointerBuffer getRequiredLayers(List<String> validationLayers, MemoryStack stack) {
      PointerBuffer requiredLayers = stack.mallocPointer(validationLayers.size());
      for (int i = 0; i < validationLayers.size(); i++) {
         requiredLayers.put(i, stack.ASCII(validationLayers.get(i)));
      }
      return requiredLayers;
   }
}
