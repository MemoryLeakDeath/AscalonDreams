package tv.memoryleakdeath.ascalondreams.camera;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Camera {
   private static final Logger logger = LoggerFactory.getLogger(Camera.class);
   private final Vector3f direction = new Vector3f();
   private final Vector3f position = new Vector3f();
   private final Vector3f right = new Vector3f();
   private final Vector2f rotation = new Vector2f();
   private final Vector3f up = new Vector3f();
   private final Matrix4f viewMatrix = new Matrix4f();

   public void addRotation(float x, float y) {
      rotation.add(x, y);
      recalculateMatrix();
   }

   private void recalculateMatrix() {
      viewMatrix.identity()
              .rotateX(rotation.x)
              .rotateY(rotation.y)
              .translate(-position.x, -position.y, -position.z);
   }

   public void moveBackwards(float inc) {
      viewMatrix.positiveZ(direction).negate().mul(inc);
      position.sub(direction);
      recalculateMatrix();
   }

   public void moveDown(float inc) {
      viewMatrix.positiveY(up).mul(inc);
      position.sub(up);
      recalculateMatrix();
   }

   public void moveForward(float inc) {
      viewMatrix.positiveZ(direction).negate().mul(inc);
      position.add(direction);
      recalculateMatrix();
   }

   public void moveLeft(float inc) {
      viewMatrix.positiveX(right).mul(inc);
      position.sub(right);
      recalculateMatrix();
   }

   public void moveRight(float inc) {
      viewMatrix.positiveX(right).mul(inc);
      position.add(right);
      recalculateMatrix();
   }

   public void moveUp(float inc) {
      viewMatrix.positiveY(up).mul(inc);
      position.add(up);
      recalculateMatrix();
   }

   public void setPosition(float x, float y, float z) {
      position.set(x, y, z);
      recalculateMatrix();
   }

   public void setRotation(float x, float y) {
      rotation.set(x, y);
      recalculateMatrix();
   }

   public Vector3f getPosition() {
      return position;
   }

   public Matrix4f getViewMatrix() {
      return viewMatrix;
   }
}
