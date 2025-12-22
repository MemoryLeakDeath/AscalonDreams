package tv.memoryleakdeath.ascalondreams.camera;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import tv.memoryleakdeath.ascalondreams.input.KeyboardInputCallback;
import tv.memoryleakdeath.ascalondreams.input.MouseInputCallback;
import tv.memoryleakdeath.ascalondreams.state.GameState;

import java.util.Set;

public class CameraInputCallback implements KeyboardInputCallback, MouseInputCallback {
   private static final float MOUSE_SENSITIVITY = 0.1f;
   private static final float MOVEMENT_SPEED = 0.01f;
   private static final Set<Integer> KEYS = Set.of(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN);
   private Camera camera;
   private Set<Integer> pressedKeys;
   private Vector2f currentCursorPosition;
   private Vector2f deltaCursorPosition;
   private boolean[] buttonsPressed;
   private boolean mouseMoved = false;

   public CameraInputCallback(Camera camera) {
      this.camera = camera;
   }
   @Override
   public boolean handles(Set<Integer> pressedKeys, int pressedModifiers, GameState state) {
      // keyboard input handler
      boolean isHandled = KEYS.stream().anyMatch(pressedKeys::contains);
      if(isHandled) {
         this.pressedKeys = pressedKeys;
      }
      return isHandled;
   }

   @Override
   public void performAction(long deltaTimeMillis) {
      float move = deltaTimeMillis * MOVEMENT_SPEED;
      if(pressedKeys != null) {
         if(pressedKeys.contains(GLFW.GLFW_KEY_W)) {
            camera.moveForward(move);
         } else if(pressedKeys.contains(GLFW.GLFW_KEY_S)) {
            camera.moveBackwards(move);
         }

         if(pressedKeys.contains(GLFW.GLFW_KEY_A)) {
            camera.moveLeft(move);
         } else if(pressedKeys.contains(GLFW.GLFW_KEY_D)) {
            camera.moveRight(move);
         }

         if(pressedKeys.contains(GLFW.GLFW_KEY_UP)) {
            camera.moveUp(move);
         } else if(pressedKeys.contains(GLFW.GLFW_KEY_DOWN)) {
            camera.moveDown(move);
         }
      }
      if(mouseMoved) {
         camera.addRotation((float) Math.toRadians(-deltaCursorPosition.y * MOUSE_SENSITIVITY),
                 (float) Math.toRadians(-deltaCursorPosition.x * MOUSE_SENSITIVITY));
      }
   }

   @Override
   public boolean handles(Vector2f currentCursorPosition, Vector2f deltaCursorPosition, boolean[] buttonsPressed, int modifiersPressed) {
      // mouse input handler
      boolean isHandled = (deltaCursorPosition.x >= 0 || deltaCursorPosition.y >= 0);
      if(isHandled) {
         this.currentCursorPosition = currentCursorPosition;
         this.deltaCursorPosition = deltaCursorPosition;
         this.buttonsPressed = buttonsPressed;
         this.mouseMoved = true;
      } else {
         this.mouseMoved = false;
      }
      return isHandled;
   }
}
