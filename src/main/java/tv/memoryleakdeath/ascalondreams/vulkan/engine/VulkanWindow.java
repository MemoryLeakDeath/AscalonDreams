package tv.memoryleakdeath.ascalondreams.vulkan.engine;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallbackHandler;
import tv.memoryleakdeath.ascalondreams.input.MouseCallbackHandler;

public class VulkanWindow {
    private static final Logger logger = LoggerFactory.getLogger(VulkanWindow.class);
    private static final String WINDOW_TITLE = "Ascalon Dreams";
    private static final float FOV = (float) Math.toRadians(60.0f);
    private static final float ZNEAR = 0.5f;
    private static final float ZFAR = 50.0f;

    private int width;
    private int height;
    private long handle;

    public VulkanWindow(int width, int height) {
        this.width = width;
        this.height = height;
        init();
    }

    private void init() {
        if (!GLFW.glfwInit()) {
            logger.error("GLFW failed to init!");
            throw new IllegalStateException("GLFW failed to init!");
        }
        if (!GLFWVulkan.glfwVulkanSupported()) {
            logger.error("Vulkan is not supported!");
            throw new IllegalStateException("Vulkan is not supported on this hardware!");
        }
        GLFW.glfwSetErrorCallback((int errorCode, long messagePointer) -> {
            logger.error("Error code: {} Message: {}", errorCode, MemoryUtil.memUTF8(messagePointer));
        });

        // configure window
        GLFW.glfwDefaultWindowHints();
        //GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        //GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, GLFW.GLFW_FALSE);

       // get resolution of primary monitor
       GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
       if(vidMode == null) {
          throw new RuntimeException("Error getting primary monitor");
       }
       width = vidMode.width();
       height = vidMode.height();

        // create window
        handle = GLFW.glfwCreateWindow(width, height, WINDOW_TITLE, MemoryUtil.NULL, MemoryUtil.NULL);
        if (handle == MemoryUtil.NULL) {
            logger.error("Unable to create game window!");
            throw new RuntimeException("Unable to create game window!");
        }

        // center window
        //GLFW.glfwSetWindowPos(handle, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);
       GLFW.glfwSetFramebufferSizeCallback(handle, ((window, w, h) -> {
          width = w;
          height = h;
       }));

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
       KeyboardCallbackHandler.getInstance(handle).input();
       MouseCallbackHandler.getInstance(handle).input();
    }

    public void update() {
        // GLFW.glfwSwapBuffers(handle);
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

    public float getFov() {
       return FOV;
    }

    public float getZNear() {
       return ZNEAR;
    }

    public float getZFar() {
       return ZFAR;
    }
}
