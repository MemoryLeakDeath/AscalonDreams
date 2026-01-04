package tv.memoryleakdeath.ascalondreams.scene;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Projection {
   private static final Logger logger = LoggerFactory.getLogger(Projection.class);

   private final float fov;
   private final Matrix4f projectionMatrix = new Matrix4f();
   private final float zFar;
   private final float zNear;

   public Projection(float fov, float zNear, float zFar, int width, int height) {
      this.fov = fov;
      this.zNear = zNear;
      this.zFar = zFar;
      resize(width, height);
   }

   public void resize(int width, int height) {
      projectionMatrix.identity();
      projectionMatrix.perspective(fov, (float) width / (float) height, zNear, zFar, true);
   }

   public float getFov() {
      return fov;
   }

   public Matrix4f getProjectionMatrix() {
      return projectionMatrix;
   }

   public float getzFar() {
      return zFar;
   }

   public float getzNear() {
      return zNear;
   }
}
