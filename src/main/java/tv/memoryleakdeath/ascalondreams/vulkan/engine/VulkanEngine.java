package tv.memoryleakdeath.ascalondreams.vulkan.engine;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.asset.Model;
import tv.memoryleakdeath.ascalondreams.asset.ModelLoader;
import tv.memoryleakdeath.ascalondreams.asset.ModelRenderer;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;
import tv.memoryleakdeath.ascalondreams.input.UserInputCallback;

public class VulkanEngine {
    private static final Logger logger = LoggerFactory.getLogger(VulkanEngine.class);
    public static final String MODEL_FILE = "/home/mem/development/models/scifi-ship/FBX/ship.fbx";
    private static final int LOGIC_UPDATES_PER_SECOND = 30;
    private static final int DEFAULT_FRAMES_PER_SECOND = 60;
    private static final long LOGIC_FRAME_TIME = 1_000_000_000L / LOGIC_UPDATES_PER_SECOND;
    private static final long FPS_FRAME_TIME = 1_000_000_000L / DEFAULT_FRAMES_PER_SECOND;
    private static final float MOVEMENT_INCREMENT = 0.02f;

    private VulkanWindow window;
    private Model model;
    private ModelRenderer renderer;
    private long lastLogicUpdateTimer;
    private long lastFrameUpdateTimer;

    public void init() {
        window = new VulkanWindow(600, 600);
    }

    private void loadModel() {
        ModelLoader loader = new ModelLoader();
        model = loader.load("ship", MODEL_FILE);
    }

    public void mainLoop() {
        GLFW.glfwSetKeyCallback(window.getHandle(), registerKeyboardCallbacks());

        // rendering loop
        while (!window.shouldClose()) {
            if (shouldRunLogic()) {
            }
            if (shouldRender()) {
                render();
                window.update();
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            window.pollEvents();
        }
        cleanup();
    }

    private boolean shouldRender() {
        long now = System.nanoTime();
        if (now - lastFrameUpdateTimer >= FPS_FRAME_TIME) {
            lastFrameUpdateTimer = now;
            return true;
        }
        return false;
    }

    private boolean shouldRunLogic() {
        long now = System.nanoTime();
        if (now - lastLogicUpdateTimer >= LOGIC_FRAME_TIME) {
            lastLogicUpdateTimer = now;
            return true;
        }
        return false;
    }

    private void render() {

    }

    private void cleanup() {
        window.cleanup();
    }

    private KeyboardCallback registerKeyboardCallbacks() {
        KeyboardCallback kb = new KeyboardCallback();
        kb.addHandler(GLFW.GLFW_KEY_ESCAPE, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_RELEASE) {
                    window.signalClose();
                }
            }
        }).addHandler(GLFW.GLFW_KEY_RIGHT, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_PRESS) {
                    // renderer.getCamera().orbitRight(MOVEMENT_INCREMENT);
                }
            }
        }).addHandler(GLFW.GLFW_KEY_LEFT, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_PRESS) {
                    // renderer.getCamera().orbitLeft(MOVEMENT_INCREMENT);
                }
            }
        }).addHandler(GLFW.GLFW_KEY_UP, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_PRESS) {
                    // renderer.getCamera().orbitUp(MOVEMENT_INCREMENT);
                }
            }
        }).addHandler(GLFW.GLFW_KEY_DOWN, new UserInputCallback() {
            @Override
            public void performAction(int action) {
                if (action == GLFW.GLFW_PRESS) {
                    // renderer.getCamera().orbitDown(MOVEMENT_INCREMENT);
                }
            }
        });
        return kb;
    }
}
