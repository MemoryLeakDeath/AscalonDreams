package tv.memoryleakdeath.ascalondreams.opengl.engine;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenGLWindow {
    private static final Logger logger = LoggerFactory.getLogger(OpenGLWindow.class);
    private static final String WINDOW_TITLE = "Ascalon Dreams";

    private int width;
    private int height;
    private long handle;

    public OpenGLWindow(int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    private void init() {
        if (!GLFW.glfwInit()) {
            logger.error("GLFW failed to init!");
            throw new IllegalStateException("GLFW failed to init!");
        }
        GLFW.glfwSetErrorCallback((int errorCode, long messagePointer) -> {
            logger.error("Error code: {} Message: {}", errorCode, MemoryUtil.memUTF8(messagePointer));
        });

        // configure window
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // create window
        handle = GLFW.glfwCreateWindow(width, height, WINDOW_TITLE, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) {
            logger.error("Unable to create game window!");
            throw new RuntimeException("Unable to create game window!");
        }
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_STICKY_KEYS, GLFW.GLFW_TRUE);

        // get resolution of primary monitor
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        // center window
        GLFW.glfwSetWindowPos(handle, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);

        // Make opengl context current
        GLFW.glfwMakeContextCurrent(handle);

        // Enable v-sync
        GLFW.glfwSwapInterval(1);

        // make window visible
        GLFW.glfwShowWindow(handle);
    }

    public void cleanup() {
        // free window callbacks and destroy window
        Callbacks.glfwFreeCallbacks(handle);
        GLFW.glfwDestroyWindow(handle);

        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    public void pollEvents() {
        GLFW.glfwPollEvents(); // poll for window/input events
    }

    public void update() {
        GLFW.glfwSwapBuffers(handle);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    public void signalClose() {
        GLFW.glfwSetWindowShouldClose(handle, true);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getHandle() {
        return handle;
    }
}
