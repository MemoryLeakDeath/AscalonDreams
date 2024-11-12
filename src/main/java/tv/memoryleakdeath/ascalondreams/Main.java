package tv.memoryleakdeath.ascalondreams;

import java.io.PrintStream;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import tv.memoryleakdeath.ascalondreams.asset.Model;
import tv.memoryleakdeath.ascalondreams.asset.ModelLoader;
import tv.memoryleakdeath.ascalondreams.asset.ModelRenderer;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;
import tv.memoryleakdeath.ascalondreams.input.UserInputCallback;
import tv.memoryleakdeath.ascalondreams.util.LoggingPrintStream;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String MODEL_FILE = "/home/mem/development/models/scifi-ship/FBX/ship.fbx";

    private long windowHandle;
    private Model model;

    private void run() {
        logger.info("Starting program....");
        init();
        mainLoop();
        logger.info("Exiting program....");
        terminateProgram();
    }

    private void init() {
        GLFWErrorCallback.createPrint(new PrintStream(new LoggingPrintStream(Level.ERROR, getClass()))).set();
        if (!GLFW.glfwInit()) {
            logger.error("GLFW failed to init!");
            throw new IllegalStateException("GLFW failed to init!");
        }

        // configure window
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        // create window
        windowHandle = GLFW.glfwCreateWindow(600, 600, "Ascalon Dreams", MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            logger.error("Unable to create game window!");
            throw new RuntimeException("Unable to create game window!");
        }

        loadModel();

        GLFW.glfwSetKeyCallback(windowHandle, registerKeyboardCallbacks());

        // get resolution of primary monitor
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        // center window
        GLFW.glfwSetWindowPos(windowHandle, (vidMode.width() - 600) / 2, (vidMode.height() - 600) / 2);

        // Make opengl context current
        GLFW.glfwMakeContextCurrent(windowHandle);

        // Enable v-sync
        GLFW.glfwSwapInterval(1);

        // make window visible
        GLFW.glfwShowWindow(windowHandle);
    }

    private void loadModel() {
        ModelLoader loader = new ModelLoader();
        // model = loader.load(MODEL_FILE);
    }

    private void mainLoop() {
        GL.createCapabilities();
        GL46.glClearColor(0f, 0f, 0f, 0f);

        ModelRenderer renderer = new ModelRenderer();
        // model.setCurrentRotation(0.0f);

        // rendering loop
        while (!GLFW.glfwWindowShouldClose(windowHandle)) {
            GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT); // clear framebuffers

            GL46.glPushMatrix();
            // GL46.glRotatef(model.getCurrentRotation(), 0f, 1f, 0f);
            renderer.render(model);
            GL46.glPopMatrix();

            GLFW.glfwSwapBuffers(windowHandle);
            GLFW.glfwPollEvents(); // poll for window/input events
        }
    }

    private void terminateProgram() {
        // free window callbacks and destroy window
        Callbacks.glfwFreeCallbacks(windowHandle);
        GLFW.glfwDestroyWindow(windowHandle);

        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    private KeyboardCallback registerKeyboardCallbacks() {
        KeyboardCallback kb = new KeyboardCallback();
        kb.addHandler(GLFW.GLFW_KEY_ESCAPE, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_RELEASE) {
                    GLFW.glfwSetWindowShouldClose(windowHandle, true);
                }
            }
        }).addHandler(GLFW.GLFW_KEY_RIGHT, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_PRESS) {
                    // model.setCurrentRotation(model.getCurrentRotation() + 0.08f);
                }
            }
        }).addHandler(GLFW.GLFW_KEY_LEFT, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_PRESS) {
                    // model.setCurrentRotation(model.getCurrentRotation() - 0.08f);
                }
            }
        });
        return kb;
    }

    public static void main(String[] args) {
        new Main().run();
    }

}
