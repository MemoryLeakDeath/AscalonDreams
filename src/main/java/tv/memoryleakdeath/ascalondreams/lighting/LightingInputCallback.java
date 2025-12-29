package tv.memoryleakdeath.ascalondreams.lighting;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import tv.memoryleakdeath.ascalondreams.input.KeyboardInputCallback;
import tv.memoryleakdeath.ascalondreams.state.GameState;
import tv.memoryleakdeath.ascalondreams.state.StateMachine;

import java.util.Set;

public class LightingInputCallback implements KeyboardInputCallback {
   private static final Set<Integer> KEYS = Set.of(GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_KP_8, GLFW.GLFW_KEY_KP_4, GLFW.GLFW_KEY_KP_6, GLFW.GLFW_KEY_KP_2);
   private Light directionalLight;
   private Light pointLight;
   private float directionalLightAngle = 270f;
   private Set<Integer> pressedKeys;
   private StateMachine stateMachine = StateMachine.getInstance();

   public LightingInputCallback(Light directionalLight, Light pointLight) {
      this.directionalLight = directionalLight;
      this.pointLight = pointLight;
   }

   @Override
   public boolean handles(Set<Integer> pressedKeys, int pressedModifiers, GameState state) {
      if(stateMachine.getCurrentGameState() == GameState.GUI) {
         return false;
      }

      // keyboard input handler
      boolean isHandled = KEYS.stream().anyMatch(pressedKeys::contains);
      if(isHandled) {
         this.pressedKeys = pressedKeys;
      }
      return isHandled;
   }

   @Override
   public void performAction(long deltaTimeMillis) {
      float angleIncrement = 0f;
      float move = 0.1f;
      if(pressedKeys != null) {
         if(pressedKeys.contains(GLFW.GLFW_KEY_LEFT)) {
            angleIncrement -= 0.05f;
         } else if(pressedKeys.contains(GLFW.GLFW_KEY_RIGHT)) {
            angleIncrement += 0.05f;
         } else {
            angleIncrement = 0;
         }
         if(angleIncrement != 0) {
            directionalLightAngle += angleIncrement;
            if(directionalLightAngle < 180) {
               directionalLightAngle = 180;
            } else if(directionalLightAngle > 360) {
               directionalLightAngle = 360;
            }
            updateDirectionalLight();
         }

         if(pressedKeys.contains(GLFW.GLFW_KEY_KP_8)) {
            pointLight.position().y += move;
         } else if(pressedKeys.contains(GLFW.GLFW_KEY_KP_2)) {
            pointLight.position().y -= move;
         }

         if(pressedKeys.contains(GLFW.GLFW_KEY_KP_4)) {
            pointLight.position().z -= move;
         } else if(pressedKeys.contains(GLFW.GLFW_KEY_KP_6)) {
            pointLight.position().z += move;
         }

      }
   }

   private void updateDirectionalLight() {
      float zValue = (float) Math.cos(Math.toRadians(directionalLightAngle));
      float yValue = (float) Math.sin(Math.toRadians(directionalLightAngle));
      Vector3f lightDirection = directionalLight.position();
      lightDirection.x = 0;
      lightDirection.y = yValue;
      lightDirection.z = zValue;
      lightDirection.normalize();
   }
}
