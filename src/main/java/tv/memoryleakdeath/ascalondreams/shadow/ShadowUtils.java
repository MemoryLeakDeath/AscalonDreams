package tv.memoryleakdeath.ascalondreams.shadow;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.camera.Camera;
import tv.memoryleakdeath.ascalondreams.lighting.Light;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.Projection;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

public class ShadowUtils {
   private static final Logger logger = LoggerFactory.getLogger(ShadowUtils.class);
   private static final float LAMBDA = 0.95f;
   private static final Vector3f UP = new Vector3f(0f, 1f, 0f);
   private static final Vector3f UP_ALT = new Vector3f(0f, 0f, 1f);
   public static final int SHADOW_MAP_SIZE = 4096;

   private ShadowUtils() {
   }

   // Function are derived from Vulkan examples from Sascha Willems, and licensed under the MIT License:
   // https://github.com/SaschaWillems/Vulkan/tree/master/examples/shadowmappingcascade, which are based on
   // https://johanmedestrom.wordpress.com/2016/03/18/opengl-cascaded-shadow-maps/
   // combined with this source: https://github.com/TheRealMJP/Shadows
   public static void updateCascadeShadows(CascadeShadows cascadeShadows, VulkanScene scene) {
      Camera camera = scene.getCamera();
      Projection projection = scene.getProjection();
      Light directionalLight = scene.getLights().stream().filter(Light::directional).findFirst().orElse(null);
      if(directionalLight == null) {
         throw new RuntimeException("Could not find directional light source in scene!");
      }
      Vector3f lightPosition = directionalLight.position();

      float[] cascadeSplits = new float[VulkanScene.SHADOW_MAP_CASCADE_COUNT];
      float nearClip = projection.getzNear();
      float farClip = projection.getzFar();
      float clipRange = farClip - nearClip;
      float minZ = nearClip;
      float maxZ = nearClip + clipRange;
      float range = maxZ - minZ;
      float ratio = maxZ / minZ;

      var cascadeDataList = cascadeShadows.getCascadeData();
      int numCascades = cascadeDataList.size();

      // Calculate split depths based on view camera frustum
      // Based on method presented in https://developer.nvidia.com/gpugems/GPUGems3/gpugems3_ch10.html
      for(int i = 0; i < numCascades; i++) {
         float p = (i + 1) / (float) VulkanScene.SHADOW_MAP_CASCADE_COUNT;
         float log = (float) (minZ * Math.pow(ratio, p));
         float uniform = minZ + range * p;
         float d = LAMBDA * (log - uniform) + uniform;
         cascadeSplits[i] = (d - nearClip) / clipRange;
      }

      // calculate orthographic projection matrix for each shadow cascade
      float lastSplitDistance = 0.0f;
      Vector3f[] initialViewingFrustumCorners = initFrustumToWorldCoords(projection.getProjectionMatrix(), camera.getViewMatrix());
      for(int i = 0; i < numCascades; i++) {
         float splitDistance = cascadeSplits[i];

         Vector3f[] viewingFrustumCorners = new Vector3f[initialViewingFrustumCorners.length];
         for(int j = 0; j < viewingFrustumCorners.length; j++) {
            viewingFrustumCorners[j] = new Vector3f(initialViewingFrustumCorners[j]);
         }

         for(int j = 0; j < 4; j++) {
            var dist = new Vector3f(viewingFrustumCorners[j + 4].sub(viewingFrustumCorners[j]));
            viewingFrustumCorners[j + 4] = new Vector3f(viewingFrustumCorners[j]).add(new Vector3f(dist).mul(splitDistance));
            viewingFrustumCorners[j] = new Vector3f(viewingFrustumCorners[j]).add(new Vector3f(dist).mul(lastSplitDistance));
         }

         // Get frustum center
         var frustumCenter = new Vector3f(0f);
         for(int j = 0; j < viewingFrustumCorners.length; j++) {
            frustumCenter.add(viewingFrustumCorners[j]);
         }
         frustumCenter.div((float)viewingFrustumCorners.length);

         var up = UP;
         float sphereRadius = 0f;
         for(int j = 0; j < viewingFrustumCorners.length; j++) {
            float dist = new Vector3f(viewingFrustumCorners[j]).sub(frustumCenter).length();
            sphereRadius = Math.max(sphereRadius, dist);
         }
         sphereRadius = (float) Math.ceil(sphereRadius * 16f) / 16f;

         var maxExtents = new Vector3f(sphereRadius, sphereRadius, sphereRadius);
         var minExtents = new Vector3f(maxExtents).negate();

         var lightDirection = new Vector3f(lightPosition);
         // Get position of the shadow camera
         var shadowCameraPosition = new Vector3f(frustumCenter).add(lightDirection.mul(minExtents.z));

         float dot = Math.abs(new Vector3f(lightPosition).dot(up));
         if(dot == 1f) {
            up = UP_ALT;
         }

         var lightViewMatrix = new Matrix4f().lookAt(shadowCameraPosition, frustumCenter, up);
         var lightOrthographicMatrix = new Matrix4f().ortho(minExtents.x, maxExtents.x, minExtents.y, maxExtents.y,
                 0f, maxExtents.z - minExtents.z, true);

         // stabilize shadow
         int shadowMapSize = SHADOW_MAP_SIZE;
         Vector4f shadowOrigin = new Vector4f(0f, 0f, 0f, 1f);
         lightViewMatrix.transform(shadowOrigin);
         shadowOrigin.mul(shadowMapSize / 2f);

         Vector4f roundedOrigin = new Vector4f(shadowOrigin).round();
         Vector4f roundOffset = roundedOrigin.sub(shadowOrigin);
         roundOffset.mul(2f / shadowMapSize);
         roundOffset.z = 0f;
         roundOffset.w = 0f;

         lightOrthographicMatrix.m30(lightOrthographicMatrix.m30() + roundOffset.x);
         lightOrthographicMatrix.m31(lightOrthographicMatrix.m31() + roundOffset.y);
         lightOrthographicMatrix.m32(lightOrthographicMatrix.m32() + roundOffset.z);
         lightOrthographicMatrix.m33(lightOrthographicMatrix.m33() + roundOffset.w);

         // store split distance and matrix in cascade
         CascadeData cascadeData = cascadeDataList.get(i);
         cascadeData.setSplitDistance((nearClip + splitDistance * clipRange) * -1f);
         cascadeData.setProjectionViewMatrix(lightOrthographicMatrix.mul(lightViewMatrix));

         lastSplitDistance = cascadeSplits[i];
      }
   }

   private static Vector3f[] initFrustumToWorldCoords(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
      Vector3f[] corners = new Vector3f[] {
              new Vector3f(-1f, 1f, 0f),
              new Vector3f(1f, 1f, 0f),
              new Vector3f(1f, -1f, 0f),
              new Vector3f(-1f, -1f, 0f),
              new Vector3f(-1f, 1f, 1f),
              new Vector3f(1f, 1f, 1f),
              new Vector3f(1f, -1f, 1f),
              new Vector3f(-1f, -1f, 1f)
      };

      // project frustum corners into world space
      var inverseCamera = (new Matrix4f(projectionMatrix).mul(viewMatrix)).invert();
      for(int i = 0; i < corners.length; i++) {
         Vector4f inverseCorner = new Vector4f(corners[i], 1.0f).mul(inverseCamera);
         corners[i] = new Vector3f(inverseCorner.x, inverseCorner.y, inverseCorner.z).div(inverseCorner.w);
      }
      return corners;
   }
}
