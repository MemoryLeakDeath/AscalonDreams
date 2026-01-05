package tv.memoryleakdeath.ascalondreams.model;

import org.joml.Matrix4f;

public record Bone(int id, String name, Matrix4f offsetMatrix) {
}
