package tv.memoryleakdeath.ascalondreams.input;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.scene.Camera;
import tv.memoryleakdeath.ascalondreams.sound.SoundManager;
import tv.memoryleakdeath.ascalondreams.state.GameState;
import tv.memoryleakdeath.ascalondreams.state.StateMachine;

import java.util.Set;

public class CameraInputCallback implements KeyboardInputCallback, MouseInputCallback {
   private static final Logger logger = LoggerFactory.getLogger(CameraInputCallback.class);
   private static final float MOUSE_SENSITIVITY = 0.1f;
   private static final float MOVEMENT_SPEED = 0.01f;
   private static final Set<Integer> KEYS = Set.of(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN);
   private Camera camera;
   private Set<Integer> pressedKeys;
   private Vector2f currentCursorPosition;
   private Vector2f deltaCursorPosition;
   private boolean[] buttonsPressed;
   private boolean mouseMoved = false;
   private StateMachine stateMachine = StateMachine.getInstance();

   public CameraInputCallback(Camera camera) {
      this.camera = camera;
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
         logger.trace("Camera keyboard input isHandled - {} - pressedKeys: {}", isHandled, this.pressedKeys);
      }
      return isHandled;
   }

   @Override
   public void performAction(long deltaTimeMillis) {
      float move = 0.17f;
      if(move > 0) {
         logger.trace("DeltaTime: {} Move: {}", deltaTimeMillis, move);
      }
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
         SoundManager.getInstance().updateListenerPosition(camera);
      }
      if(mouseMoved && buttonsPressed[1]) {
         Vector2f delta = getDeltaCursorPosition();
         logger.trace("Delta: {}", delta);
         camera.addRotation((float) Math.toRadians(-delta.y * MOUSE_SENSITIVITY),
                 (float) Math.toRadians(-delta.x * MOUSE_SENSITIVITY));
      }
   }

   @Override
   public boolean handles(Vector2f currentCursorPosition, Vector2f deltaCursorPosition, boolean[] buttonsPressed, int modifiersPressed) {
      if(stateMachine.getCurrentGameState() == GameState.GUI) {
         return false;
      }

      // mouse input handler
      boolean isHandled = (deltaCursorPosition.x >= 0 || deltaCursorPosition.y >= 0);
      if(isHandled) {
         this.currentCursorPosition = currentCursorPosition;
         setDeltaCursorPosition(deltaCursorPosition);
         this.buttonsPressed = buttonsPressed;
         this.mouseMoved = true;
      } else {
         this.mouseMoved = false;
      }
      return isHandled;
   }

   public synchronized Vector2f getDeltaCursorPosition() {
      return deltaCursorPosition;
   }

   public synchronized void setDeltaCursorPosition(Vector2f deltaCursorPosition) {
      this.deltaCursorPosition = deltaCursorPosition;
   }
}
