package tv.memoryleakdeath.ascalondreams.vulkan.engine.model;

import org.joml.Matrix4f;

import java.util.List;

public record AnimatedFrame(List<Matrix4f> jointMatrices) {
}
