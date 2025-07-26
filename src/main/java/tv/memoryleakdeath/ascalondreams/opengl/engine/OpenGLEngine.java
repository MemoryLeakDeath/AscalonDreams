package tv.memoryleakdeath.ascalondreams.opengl.engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.common.model.Model;
import tv.memoryleakdeath.ascalondreams.common.asset.ModelLoader;
import tv.memoryleakdeath.ascalondreams.asset.ModelRenderer;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;

import java.util.List;

public class OpenGLEngine {
    private static final Logger logger = LoggerFactory.getLogger(OpenGLEngine.class);
    public static final String MODEL_FILE = "/home/memdev/development/models/scifi-ship/FBX/ship.fbx";
    private static final int LOGIC_UPDATES_PER_SECOND = 30;
    private static final int DEFAULT_FRAMES_PER_SECOND = 60;
    private static final long LOGIC_FRAME_TIME = 1_000_000_000L / LOGIC_UPDATES_PER_SECOND;
    private static final long FPS_FRAME_TIME = 1_000_000_000L / DEFAULT_FRAMES_PER_SECOND;
    private static final float MOVEMENT_INCREMENT = 0.02f;

    private OpenGLWindow window;
    private Model model;
    private ModelRenderer renderer;
    private long lastLogicUpdateTimer;
    private long lastFrameUpdateTimer;
    private KeyboardCallback kb;

    public void init() {
        window = new OpenGLWindow(600, 600);
    }

    private void loadModel() {
        ModelLoader loader = new ModelLoader();
        model = loader.load("ship", MODEL_FILE, null, List.of(ModelRenderer.TEXTURE_PATH));
    }

    public void mainLoop() {
        GL.createCapabilities();
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glCullFace(GL46.GL_BACK);
        GL46.glClearColor(0f, 0f, 0f, 0f);

        loadModel();

        renderer = new ModelRenderer();
        registerKeyboardCallbacks();

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
            kb.process();
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
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT); // clear framebuffers
        GL46.glViewport(0, 0, window.getWidth(), window.getHeight());
        renderer.render(model);
    }

    private void cleanup() {
        window.cleanup();
    }

    private void registerKeyboardCallbacks() {
        this.kb = new KeyboardCallback();
        kb.addHandler(() -> {
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                window.signalClose();
            }
        }).addHandler(() -> {
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
                renderer.getCamera().orbitRight(MOVEMENT_INCREMENT);
            }
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
                renderer.getCamera().orbitLeft(MOVEMENT_INCREMENT);
            }
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
                renderer.getCamera().orbitUp(MOVEMENT_INCREMENT);
            }
            if (GLFW.glfwGetKey(window.getHandle(), GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) {
                renderer.getCamera().orbitDown(MOVEMENT_INCREMENT);
            }
        });
    }
}
