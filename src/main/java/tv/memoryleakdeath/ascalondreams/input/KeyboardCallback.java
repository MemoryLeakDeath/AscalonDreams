package tv.memoryleakdeath.ascalondreams.input;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyboardCallback implements GLFWKeyCallbackI {
    private static final Logger logger = LoggerFactory.getLogger(KeyboardCallback.class);

    private Map<Integer, UserInputCallback> inputCallbacks = new HashMap<>();

    public KeyboardCallback addHandler(int inputKey, UserInputCallback callback) {
        inputCallbacks.put(inputKey, callback);
        return this;
    }

    @Override
    public void invoke(long window, int key, int scancode, int action, int mods) {
        if (inputCallbacks.containsKey(key)) {
            inputCallbacks.get(key).performAction(action);
        }
    }

}
