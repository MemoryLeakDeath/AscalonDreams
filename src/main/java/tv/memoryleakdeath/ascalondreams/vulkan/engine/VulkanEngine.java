package tv.memoryleakdeath.ascalondreams.vulkan.engine;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.asset.ModelLoader;
import tv.memoryleakdeath.ascalondreams.common.model.Mesh;
import tv.memoryleakdeath.ascalondreams.common.model.Model;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanRenderer;

import java.util.Collections;
import java.util.List;

public class VulkanEngine {
    private static final Logger logger = LoggerFactory.getLogger(VulkanEngine.class);
    public static final String MODEL_FILE = "/home/mem/development/models/scifi-ship/FBX/ship.fbx";
    private static final int LOGIC_UPDATES_PER_SECOND = 30;
    private static final int DEFAULT_FRAMES_PER_SECOND = 60;
    private static final long LOGIC_FRAME_TIME = 1_000_000_000L / LOGIC_UPDATES_PER_SECOND;
    private static final long FPS_FRAME_TIME = 1_000_000_000L / DEFAULT_FRAMES_PER_SECOND;
    private static final float MOVEMENT_INCREMENT = 0.02f;

    private VulkanWindow window;
    private VulkanRenderer renderer;
    private long lastLogicUpdateTimer;
    private long lastFrameUpdateTimer;
    private KeyboardCallback kb;

    public void init() {
        window = new VulkanWindow(600, 600);
        renderer = new VulkanRenderer(window);
    }

    private VulkanModel createTestModel() {
        Mesh meshData = new Mesh();
        meshData.setVertices(new float[]{
                -0.5f, -0.5f, 0f,
                0f, 0.5f, 0f,
                0.5f, -0.5f, 0f});
        meshData.setIndexes(new int[]{0, 1, 2});
        Model model = new Model("TriangleModel", List.of(meshData), Collections.emptyList());
        return new VulkanModel("TriangleModel", model);
    }

    private void loadModel() {
        ModelLoader loader = new ModelLoader();
        //model = loader.load("ship", MODEL_FILE);
    }

    public void mainLoop() {
        //GLFW.glfwSetKeyCallback(window.getHandle(), registerKeyboardCallbacks());
        renderer.loadModels(List.of(createTestModel()));

        // rendering loop
        while (!window.shouldClose()) {
            window.pollEvents();
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
        renderer.render();
    }

    private void cleanup() {
        renderer.cleanup();
        window.cleanup();
    }

    private void registerKeyboardCallbacks() {
        this.kb = new KeyboardCallback();
        kb.addHandler(() -> {
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_RELEASE) {
                window.signalClose();
            }
        }).addHandler(() -> {
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            }
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            }
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            }
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            }
        });
    }
}
