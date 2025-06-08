package tv.memoryleakdeath.ascalondreams.vulkan.engine.render;

import org.joml.Matrix4f;

public class VulkanProjection {
   private static final float FOV = 90.0f;
   private static final float Z_NEAR = 1.0f;
   private static final float Z_FAR = 100.0f;
   private Matrix4f projectionMatrix = new Matrix4f();

   public void resize(int width, int height) {
      projectionMatrix.identity()
              .perspective(FOV, (float) width / (float) height, Z_NEAR, Z_FAR, true);
   }

   public Matrix4f getProjectionMatrix() {
      return projectionMatrix;
   }
}
