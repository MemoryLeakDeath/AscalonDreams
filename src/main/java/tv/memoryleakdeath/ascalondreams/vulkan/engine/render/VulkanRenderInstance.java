package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.apache.commons.exec.OS;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.utils.VulkanUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VulkanRenderInstance {
   private static final Logger logger = LoggerFactory.getLogger(VulkanRenderInstance.class);
   public static final int MESSAGE_SEVERITY_MASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
           | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
   public static final int MESSAGE_TYPE_MASK = EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
           | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
           | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
   private static final String PORTABILITY_EXTENSION = "VK_KHR_portability_enumeration";
   private static final int VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR = 0x00000001;
   private static final List<String> VALIDATION_FALLBACK_LAYERS = List.of("VK_LAYER_GOOGLE_threading",
           "VK_LAYER_LUNARG_parameter_validation",
           "VK_LAYER_LUNARG_object_tracker",
           "VK_LAYER_LUNARG_core_validation",
           "VK_LAYER_GOOGLE_unique_objects");

   private VkInstance vkInstance;

   private VkDebugUtilsMessengerCreateInfoEXT debugUtils;
   private long vulkanDebugHandle;

   public VulkanRenderInstance(boolean validation) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         ByteBuffer appShortName = stack.UTF8("AscalonDreams");

         // app info
         VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                 .pApplicationName(appShortName)
                 .applicationVersion(1)
                 .pEngineName(appShortName)
                 .engineVersion(0)
                 .apiVersion(VK14.VK_API_VERSION_1_2);

         // validation layers
         List<String> validationLayers = getSupportedValidationLayers();
         int numValidationLayers = validationLayers.size();
         boolean supportsValidation = (validation && numValidationLayers != 0);
         if (!supportsValidation) {
            logger.debug("No validation supported!");
         }

         // set required layers
         PointerBuffer requiredLayers = null;
         if (supportsValidation) {
            requiredLayers = getRequiredLayers(validationLayers, stack);
         }


         // Extensions
         Set<String> instanceExtensions = getExtensions();
         // GLFW Extension
         PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
         if (glfwExtensions == null) {
            throw new RuntimeException("GLFW extensions were not found!");
         }
         boolean usePortabilityExt = (instanceExtensions.contains(PORTABILITY_EXTENSION) && OS.isFamilyMac());
         PointerBuffer requiredExtensions = getRequiredExtensions(instanceExtensions, supportsValidation, usePortabilityExt, glfwExtensions, stack);
         long loggingExtension = MemoryUtil.NULL;
         if (supportsValidation) {
            debugUtils = createDebugCallback();
            loggingExtension = debugUtils.address();
         }

         // create instance info
         VkInstanceCreateInfo instanceInfo = VkInstanceCreateInfo.calloc(stack)
                 .sType(VK14.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                 .pNext(loggingExtension)
                 .pApplicationInfo(appInfo)
                 .ppEnabledLayerNames(requiredLayers)
                 .ppEnabledExtensionNames(requiredExtensions);
         if (usePortabilityExt) {
            instanceInfo.flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);
         }

         PointerBuffer instanceBuf = stack.mallocPointer(1);
         VulkanUtils.failIfNeeded(VK14.vkCreateInstance(instanceInfo, null, instanceBuf), "Cannot create instance!");
         vkInstance = new VkInstance(instanceBuf.get(0), instanceInfo);

         vulkanDebugHandle = VK14.VK_NULL_HANDLE;
         if (supportsValidation) {
            LongBuffer longBuf = stack.mallocLong(1);
            VulkanUtils.failIfNeeded(EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(vkInstance, debugUtils, null, longBuf), "Cannot create debug utils!");
            vulkanDebugHandle = longBuf.get(0);
         }
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
               yield VALIDATION_FALLBACK_LAYERS.stream().filter(supportedLayers::contains).toList();
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

   private Set<String> getExtensions() {
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
      return extensions;
   }

   private PointerBuffer getRequiredExtensions(Set<String> instanceExtensions, boolean supportsValidation, boolean usePortabilityExt, PointerBuffer glfwExtensions, MemoryStack stack) {
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

   private VkDebugUtilsMessengerCreateInfoEXT createDebugCallback() {
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

   public VkInstance getVkInstance() {
      return vkInstance;
   }
}
