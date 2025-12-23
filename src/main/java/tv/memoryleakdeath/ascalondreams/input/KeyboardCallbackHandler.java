package tv.memoryleakdeath.ascalondreams.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class KeyboardCallbackHandler implements GLFWKeyCallbackI {
   private static final Logger logger = LoggerFactory.getLogger(KeyboardCallbackHandler.class);
   private static final Set<Integer> keysPressed = Collections.synchronizedSet(new LinkedHashSet<>());
   private static int modifiersPressed = -1;
   private static final List<KeyboardInputCallback> registeredCallbacks = new ArrayList<>();
   private static KeyboardCallbackHandler handler;

   private KeyboardCallbackHandler() {
   }

   public static KeyboardCallbackHandler getInstance() {
      if(handler == null) {
         handler = new KeyboardCallbackHandler();
      }
      return handler;
   }

   @Override
   public void invoke(long window, int key, int scancode, int action, int mods) {
      switch(action) {
         case GLFW.GLFW_PRESS -> keysPressed.add(key);
         case GLFW.GLFW_RELEASE -> keysPressed.remove(key);
         default -> {}
      }
      modifiersPressed = mods;
   }

   public KeyboardCallbackHandler registerCallback(KeyboardInputCallback callback) {
      registeredCallbacks.add(callback);
      return this;
   }

   public void input(long deltaTimeMillis) {
      GLFW.glfwPollEvents();
      logger.debug("Poll input deltaTime: {} keysPressed: {} registered callbacks: {}", deltaTimeMillis, keysPressed, registeredCallbacks.size());
      registeredCallbacks.stream().filter(c -> c.handles(keysPressed, modifiersPressed, null))
              .forEach(callback -> callback.performAction(deltaTimeMillis));
   }
}
