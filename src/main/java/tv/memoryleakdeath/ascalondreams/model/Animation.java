package tv.memoryleakdeath.ascalondreams.model;

import java.util.List;

public record Animation(String name, float frameTimeMillis, List<AnimatedFrame> frames) {
}
