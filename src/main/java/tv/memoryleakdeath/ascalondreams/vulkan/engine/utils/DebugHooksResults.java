package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;

public record DebugHooksResults(VkDebugUtilsMessengerCreateInfoEXT debugUtil, long loggingExtension) {
}
