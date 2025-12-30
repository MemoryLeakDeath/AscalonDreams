package tv.memoryleakdeath.ascalondreams.shadow;

import org.joml.Matrix4f;

public class CascadeData {
   private final Matrix4f projectionViewMatrix = new Matrix4f();
   private float splitDistance;

   public Matrix4f getProjectionViewMatrix() {
      return projectionViewMatrix;
   }

   public float getSplitDistance() {
      return splitDistance;
   }

   public void setSplitDistance(float splitDistance) {
      this.splitDistance = splitDistance;
   }

   public void setProjectionViewMatrix(Matrix4f projectionViewMatrix) {
      this.projectionViewMatrix.set(projectionViewMatrix);
   }
}
