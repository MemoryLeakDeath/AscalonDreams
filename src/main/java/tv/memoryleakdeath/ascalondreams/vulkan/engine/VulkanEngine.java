package tv.memoryleakdeath.ascalondreams.vulkan.engine;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.camera.CameraInputCallback;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallbackHandler;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ConvertedModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ModelLoader;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanRenderer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.Entity;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

public class VulkanEngine {
    private static final Logger logger = LoggerFactory.getLogger(VulkanEngine.class);
    public static final String MODEL_FILE = "/home/mem/development/models/scifi-ship/FBX/ship.fbx";
    private static final String CUBE_MODEL_FILE = "models/cube/cube.json";
    private static final String SPONZA_MODEL_FILE = "models/sponza/sponza.json";
    private static final int LOGIC_UPDATES_PER_SECOND = 30;
    private static final int DEFAULT_FRAMES_PER_SECOND = 60;
    private static final long LOGIC_FRAME_TIME = 1_000_000_000L / LOGIC_UPDATES_PER_SECOND;
    private static final long FPS_FRAME_TIME = 1_000_000_000L / DEFAULT_FRAMES_PER_SECOND;
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.01f;
    private final Vector3f rotationAngle = new Vector3f(1,1,1);
    private float angle = 0;

    private VulkanWindow window;
    private VulkanRenderer renderer;
    private long lastLogicUpdateTimer;
    private long lastFrameUpdateTimer;
    private KeyboardCallback kb;
    private VulkanScene scene;
    private Entity cubeEntity;
    private Entity sponzaEntity;

    public void init() {
        window = new VulkanWindow(600, 600);
        this.scene = new VulkanScene(window);
        renderer = new VulkanRenderer(window, scene);
        renderer.initModels(loadModel(SPONZA_MODEL_FILE));
        // todo: call set camera method below
    }

    private ConvertedModel loadModel(String modelFile) {
       ConvertedModel convertedModel = ModelLoader.loadModel(modelFile);
       sponzaEntity = new Entity("SponzaEntity", convertedModel.getId(), new Vector3f(0.0f, 0.0f, 0.0f));
       scene.addEntity(sponzaEntity);
       return convertedModel;
    }

    private void setCameraStartState() {
       // todo: scene getCamera (line 44 Main)
    }

    public void mainLoop() {
        //GLFW.glfwSetKeyCallback(window.getHandle(), registerKeyboardCallbacks());

        // rendering loop
       long lastInputPoll = System.currentTimeMillis();
        while (!window.shouldClose()) {
           window.pollEvents(System.currentTimeMillis() - lastInputPoll); // see bottom of loop for lastInputPoll update
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
           lastInputPoll = System.currentTimeMillis();
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
        renderer.render(scene);
    }

    private void gameLogic() {
       angle += 1.0f;
       if(angle >= 360) {
          angle = angle - 360;
       }
       cubeEntity.getRotation().identity().rotateAxis((float) Math.toRadians(angle), rotationAngle);
       cubeEntity.updateModelMatrix();
    }

    private void cleanup() {
       renderer.cleanup();
       window.cleanup();
    }

    private void registerKeyboardInputCallbacks() {
       KeyboardCallbackHandler.getInstance()
               .registerCallback(new CameraInputCallback(null)); // todo: pass camera in here
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
