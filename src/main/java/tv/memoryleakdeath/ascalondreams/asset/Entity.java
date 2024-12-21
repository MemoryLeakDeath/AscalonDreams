package tv.memoryleakdeath.ascalondreams.asset;

import java.io.Serializable;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String modelId;
    private Matrix4f modelMatrix = new Matrix4f();
    private Vector3f position = new Vector3f();
    private Quaternionf rotation = new Quaternionf();
    private float scale = 1.0f;

    public Entity(String id, String modelId) {
        this.id = id;
        this.modelId = modelId;
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Entity setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
        return this;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public Entity setRotation(float x, float y, float z, float angle) {
        rotation.fromAxisAngleRad(x, y, z, angle);
        return this;
    }

    public float getScale() {
        return scale;
    }

    public Entity setScale(float scale) {
        this.scale = scale;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getModelId() {
        return modelId;
    }

    public void updateModelMatrix() {
        modelMatrix.translationRotateScale(position, rotation, scale);
    }

}
