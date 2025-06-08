package tv.memoryleakdeath.ascalondreams.common.model;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Entity {
   private String id;
   private String modelId;
   private Matrix4f modelMatrix = new Matrix4f();
   private Vector3f position;
   private Quaternionf rotation = new Quaternionf();
   private float scale = 1.0f;
   private float angle = 0f;

   public Entity(String id, String modelId, Vector3f position) {
      this.id = id;
      this.modelId = modelId;
      this.position = position;
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

   public float getAngle() {
      return angle;
   }

   public void rotate(float amount, Vector3f rotationAngle) {
      angle += amount;
      if (angle >= 360) {
         angle -= 360;
      }
      rotation.identity().rotateAxis((float) Math.toRadians(this.angle), rotationAngle);
      updateModelMatrix();
   }

   public void resetRotation() {
      rotation.set(0f, 0f, 0f, 1f);
   }

   public void setPosition(float x, float y, float z) {
      position.set(x, y, z);
      updateModelMatrix();
   }

   public void setScale(float scale) {
      this.scale = scale;
      updateModelMatrix();
   }

   public void updateModelMatrix() {
      modelMatrix.translationRotateScale(position, rotation, scale);
   }
}
