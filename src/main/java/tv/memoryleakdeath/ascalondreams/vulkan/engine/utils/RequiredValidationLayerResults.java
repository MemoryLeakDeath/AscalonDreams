package tv.memoryleakdeath.ascalondreams.vulkan.engine.utils;

import org.lwjgl.PointerBuffer;

public record RequiredValidationLayerResults(PointerBuffer requiredLayers, boolean supportsValidation) {
}
