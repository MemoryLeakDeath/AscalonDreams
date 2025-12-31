package tv.memoryleakdeath.ascalondreams.lighting;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.input.KeyboardInputCallback;
import tv.memoryleakdeath.ascalondreams.state.GameState;
import tv.memoryleakdeath.ascalondreams.state.StateMachine;

import java.util.Set;

public class LightingInputCallback implements KeyboardInputCallback {
   private static final Logger logger = LoggerFactory.getLogger(LightingInputCallback.class);
   private static final Set<Integer> KEYS = Set.of(GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT);
   private Light directionalLight;
   private float directionalLightAngle = 270f;
   private Set<Integer> pressedKeys;
   private StateMachine stateMachine = StateMachine.getInstance();

   public LightingInputCallback(Light directionalLight) {
      this.directionalLight = directionalLight;
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
   public void performAction(long deltaTimeNano) {
      float angleIncrement = 0f;
      boolean doIncrement = (deltaTimeNano > 2500);
      if(pressedKeys != null && doIncrement) {
         if(pressedKeys.contains(GLFW.GLFW_KEY_LEFT)) {
            angleIncrement -= 0.05f;
         } else if(pressedKeys.contains(GLFW.GLFW_KEY_RIGHT)) {
            angleIncrement += 0.05f;
         } else {
            angleIncrement = 0;
         }
         if(angleIncrement != 0) {
            directionalLightAngle += angleIncrement;
            if(directionalLightAngle < 240) {
               directionalLightAngle = 240;
            } else if(directionalLightAngle > 300) {
               directionalLightAngle = 300;
            }
            updateDirectionalLight();
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
