package tv.memoryleakdeath.ascalondreams.vulkan.engine;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.asset.ModelRenderer;
import tv.memoryleakdeath.ascalondreams.common.asset.ModelLoader;
import tv.memoryleakdeath.ascalondreams.common.model.Entity;
import tv.memoryleakdeath.ascalondreams.common.model.Mesh;
import tv.memoryleakdeath.ascalondreams.common.model.Model;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.asset.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanRenderer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

import java.util.Collections;
import java.util.List;

public class VulkanEngine {
    private static final Logger logger = LoggerFactory.getLogger(VulkanEngine.class);
    public static final String MODEL_FILE = "/home/memdev/development/models/scifi-ship/FBX/ship.fbx";

    private VulkanWindow window;
    private VulkanRenderer renderer;
    private VulkanScene scene;
    private long lastLogicUpdateTimer;
    private long lastFrameUpdateTimer;
    private KeyboardCallback kb;
    private Vector3f rotationAngle = new Vector3f(1f, 1f, 1f);

    public void init() {
        logger.debug("==== VULKAN ENGINE STARTUP ====");
        window = new VulkanWindow(600, 600);
        scene = new VulkanScene(window.getWidth(), window.getHeight());
        initModel(scene);
        renderer = new VulkanRenderer(window, scene);
    }

    private void initModel(VulkanScene scene) {
       ModelLoader loader = new ModelLoader();
       Model model = loader.load("ship", MODEL_FILE, null, List.of(ModelRenderer.TEXTURE_PATH));
       createTestEntity(scene);
    }

//    private VulkanModel createTestModel() {
//        Mesh meshData = new Mesh();
//        meshData.setVertices(new float[]{
//                -0.5f, 0.5f, 0.5f,
//                -0.5f, -0.5f, 0.5f,
//                0.5f, -0.5f, 0.5f,
//                0.5f, 0.5f, 0.5f,
//                -0.5f, 0.5f, -0.5f,
//                0.5f, 0.5f, -0.5f,
//                -0.5f, -0.5f, -0.5f,
//                0.5f, -0.5f, -0.5f,
//        });
//        meshData.setTexCoords(List.of(new float[]{
//                0.0f, 0.0f,
//                0.5f, 0.0f,
//                1.0f, 0.0f,
//                1.0f, 0.5f,
//                1.0f, 1.0f,
//                0.5f, 1.0f,
//                0.0f, 1.0f,
//                0.0f, 0.5f,
//        }));
//        meshData.setIndexes(new int[]{
//                // Front face
//                0, 1, 3, 3, 1, 2,
//                // Top Face
//                4, 0, 3, 5, 4, 3,
//                // Right face
//                3, 2, 7, 5, 3, 7,
//                // Left face
//                6, 1, 0, 6, 0, 4,
//                // Bottom face
//                2, 1, 6, 2, 6, 7,
//                // Back face
//                7, 6, 4, 7, 4, 5,
//        });
//        Model model = new Model("CubeModel", List.of(meshData), Collections.emptyList());
//        return new VulkanModel("CubeModel", model);
//    }

    private void createTestEntity(VulkanScene scene) {
        Entity shipEntity = new Entity("ShipEntity", "ShipModel", new Vector3f(0f, 0f, 0f));
        shipEntity.setPosition(0f, 0f, -2f);
        scene.addEntity(shipEntity);
    }

    public void mainLoop() {
        //GLFW.glfwSetKeyCallback(window.getHandle(), registerKeyboardCallbacks());
//        renderer.loadModels(List.of(createTestModel()));
//        createTestEntity();

        // rendering loop
        while (!window.shouldClose()) {
            window.pollEvents();
            if (shouldRunLogic()) {
            }
            if (shouldRender()) {
                render();
                update();
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
        if (now - lastFrameUpdateTimer >= VulkanEngineConfiguration.getInstance().getFpsFrameTime()) {
            lastFrameUpdateTimer = now;
            return true;
        }
        return false;
    }

    private boolean shouldRunLogic() {
        long now = System.nanoTime();
        if (now - lastLogicUpdateTimer >= VulkanEngineConfiguration.getInstance().getLogicFrameTime()) {
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

    private void update() {
        scene.getEntity("CubeModel", "CubeEntity").rotate(1f, rotationAngle);
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
