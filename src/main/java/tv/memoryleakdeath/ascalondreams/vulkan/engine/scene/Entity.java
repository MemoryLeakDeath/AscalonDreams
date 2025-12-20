package tv.memoryleakdeath.ascalondreams.vulkan.engine.scene;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Entity {
   private static final Logger logger = LoggerFactory.getLogger(Entity.class);

   private final String id;
   private final String modelId;
   private final Matrix4f modelMatrix = new Matrix4f();
   private final Vector3f position;
   private final Quaternionf rotation = new Quaternionf();
   private float scale = 1f;

   public Entity(String id, String modelId, Vector3f position) {
      this.id = id;
      this.modelId = modelId;
      this.position = position;
      updateModelMatrix();
   }

   public void updateModelMatrix() {
      modelMatrix.translationRotateScale(position, rotation, scale);
   }

   public void resetRotation() {
      rotation.x = 0f;
      rotation.y = 0f;
      rotation.z = 0f;
      rotation.w = 1f;
   }

   public final void setPosition(float x, float y, float z) {
      position.x = x;
      position.y = y;
      position.z = z;
      updateModelMatrix();
   }

   public void setScale(float scale) {
      this.scale = scale;
      updateModelMatrix();
   }

   public String getId() {
      return id;
   }

   public String getModelId() {
      return modelId;
   }

   public Matrix4f getModelMatrix() {
      return modelMatrix;
   }

   public Vector3f getPosition() {
      return position;
   }

   public Quaternionf getRotation() {
      return rotation;
   }

   public float getScale() {
      return scale;
   }
}
