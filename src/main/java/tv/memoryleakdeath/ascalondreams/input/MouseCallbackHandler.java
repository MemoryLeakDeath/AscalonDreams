package tv.memoryleakdeath.ascalondreams.input;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorEnterCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MouseCallbackHandler implements GLFWCursorPosCallbackI {
   private static final Logger logger = LoggerFactory.getLogger(MouseCallbackHandler.class);
   private static Vector2f currentCursorPosition = new Vector2f();
   private static Vector2f previousCursorPosition = new Vector2f(-1f, -1f);
   private static Vector2f deltaCursorPosition = new Vector2f();
   private static boolean cursorInWindow = false;
   private static boolean[] buttonsPressed = new boolean[8];
   private static int modifierKeys = -1;
   private static List<MouseInputCallback> callbacks = new ArrayList<>();
   private static MouseCallbackHandler handler;
   private MouseEnteredHandler enteredHandler;
   private MouseButtonHandler buttonHandler;
   private static long windowHandle;

   private MouseCallbackHandler(long windowHandle) {
      this.windowHandle = windowHandle;
      this.enteredHandler = new MouseEnteredHandler();
      this.buttonHandler = new MouseButtonHandler();
   }

   public static MouseCallbackHandler getInstance(long windowHandle) {
      if(handler == null) {
         handler = new MouseCallbackHandler(windowHandle);
      }
      return handler;
   }

   public MouseEnteredHandler getEnteredHandler() {
      return enteredHandler;
   }

   public MouseButtonHandler getButtonHandler() {
      return buttonHandler;
   }

   @Override
   public void invoke(long window, double xpos, double ypos) {
      if(window != windowHandle) {
         return;
      }
      // cursor moved invocation
      currentCursorPosition.x = (float) xpos;
      currentCursorPosition.y = (float) ypos;
   }

   public MouseCallbackHandler registerCallback(MouseInputCallback callback) {
      callbacks.add(callback);
      return this;
   }

   public void input() {
      deltaCursorPosition.x = 0f;
      deltaCursorPosition.y = 0f;
      if(previousCursorPosition.x >= 0 && previousCursorPosition.y >= 0 && cursorInWindow) {
         deltaCursorPosition.x = currentCursorPosition.x - previousCursorPosition.x;
         deltaCursorPosition.y = currentCursorPosition.y - previousCursorPosition.y;
      }
      previousCursorPosition.x = currentCursorPosition.x;
      previousCursorPosition.y = currentCursorPosition.y;

      if(cursorInWindow) {
         callbacks.stream().filter(c -> c.handles(currentCursorPosition, deltaCursorPosition, buttonsPressed, modifierKeys))
                 .forEach(callback -> callback.performAction(InputTimer.getInstance().delta()));
      }
   }

   class MouseEnteredHandler implements GLFWCursorEnterCallbackI {

      @Override
      public void invoke(long window, boolean entered) {
         if(window != windowHandle) {
            return;
         }
         // cursor entered invocation
         cursorInWindow = entered;
      }
   }

   class MouseButtonHandler implements GLFWMouseButtonCallbackI {
      @Override
      public void invoke(long window, int button, int action, int mods) {
         if(window != windowHandle) {
            return;
         }
         // mouse button invocations
         switch(action) {
            case GLFW.GLFW_PRESS -> buttonsPressed[button] = true;
            case GLFW.GLFW_RELEASE -> buttonsPressed[button] = false;
            default -> {}
         }
         modifierKeys = mods;
      }
   }
}
