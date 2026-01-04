package tv.memoryleakdeath.ascalondreams.model;

import org.joml.Matrix4f;

import java.util.List;

public record AnimatedFrame(List<Matrix4f> jointMatrices) {
}
