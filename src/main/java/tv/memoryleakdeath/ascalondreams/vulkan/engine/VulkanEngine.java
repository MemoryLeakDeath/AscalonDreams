package tv.memoryleakdeath.ascalondreams.vulkan.engine;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.asset.Model;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.VulkanModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanRenderer;

import java.util.List;

public class VulkanEngine {
    private static final Logger logger = LoggerFactory.getLogger(VulkanEngine.class);
    public static final String MODEL_FILE = "/home/mem/development/models/scifi-ship/FBX/ship.fbx";
    private static final int LOGIC_UPDATES_PER_SECOND = 30;
    private static final int DEFAULT_FRAMES_PER_SECOND = 60;
    private static final long LOGIC_FRAME_TIME = 1_000_000_000L / LOGIC_UPDATES_PER_SECOND;
    private static final long FPS_FRAME_TIME = 1_000_000_000L / DEFAULT_FRAMES_PER_SECOND;
    private static final float MOVEMENT_INCREMENT = 0.02f;
    private final Vector3f rotationAngle = new Vector3f(1,1,1);
    private float angle = 0;
    // TODO: cube entity

    private VulkanWindow window;
    private VulkanRenderer renderer;
    private Model model;
    private long lastLogicUpdateTimer;
    private long lastFrameUpdateTimer;
    private KeyboardCallback kb;

    public void init() {
        window = new VulkanWindow(600, 600);
        renderer = new VulkanRenderer(window);
        renderer.initModels(loadModel());
    }

    private List<VulkanModel> loadModel() {
       float[] verticies = new float[] {
               -0.5f, 0.5f, 0.5f,
               -0.5f, -0.5f, 0.5f,
               0.5f, -0.5f, 0.5f,
               0.5f, 0.5f, 0.5f,
               -0.5f, 0.5f, -0.5f,
               0.5f, 0.5f, -0.5f,
               -0.5f, -0.5f, -0.5f,
               0.5f, -0.5f, -0.5f
       };
       float[] textureCoords = new float[] {
               0f, 0f,
               0.5f, 0f,
               1f, 0f,
               1f, 0.5f,
               1f, 1f,
               0.5f, 1f,
               0f, 1f,
               0f, 0.5f
       };
       int[] indicies = new int[] {
               // front
               0, 1, 3, 3, 1, 2,
               // top
               4, 0, 3, 5, 4, 3,
               // right
               3, 2, 7, 5, 3, 7,
               // left
               6, 1, 0, 6, 0, 4,
               // bottom
               2, 1, 6, 2, 6, 7,
               // back
               7, 6, 4, 7, 4, 5
       };
       String modelId = "Cube";
       VulkanModel model = new VulkanModel(modelId);
       model.addMesh(renderer.getDevice(), "cube-mesh", verticies, textureCoords, indicies);
       // todo: create entity
       // todo: scene addEntity?
       return List.of(model);
    }

    public void mainLoop() {
        //GLFW.glfwSetKeyCallback(window.getHandle(), registerKeyboardCallbacks());

        // rendering loop
        while (!window.shouldClose()) {
           window.pollEvents();
           if (shouldRunLogic()) {
              gameLogic();
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

    private void gameLogic() {
       angle += 1.0f;
       if(angle >= 360) {
          angle = angle - 360;
       }
       // todo: entity update model matrix
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
