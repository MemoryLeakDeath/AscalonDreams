package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import java.util.List;

public record Animation(String name, float frameTimeMillis, List<AnimatedFrame> frames) {
}
