package tv.memoryleakdeath.ascalondreams.render.lighting;

import org.joml.Vector3f;

public record Light(Vector3f position, boolean directional, float intensity, Vector3f color) {
}
