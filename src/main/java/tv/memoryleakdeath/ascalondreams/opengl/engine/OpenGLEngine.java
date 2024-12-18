package tv.memoryleakdeath.ascalondreams.opengl.engine;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.asset.Model;
import tv.memoryleakdeath.ascalondreams.asset.ModelLoader;
import tv.memoryleakdeath.ascalondreams.asset.ModelRenderer;

public class OpenGLEngine {
    private static final Logger logger = LoggerFactory.getLogger(OpenGLEngine.class);
    public static final String MODEL_FILE = "/home/mem/development/models/scifi-ship/FBX/ship.fbx";
    private static final int LOGIC_UPDATES_PER_SECOND = 1000 / 30;
    private static final int DEFAULT_FRAMES_PER_SECOND = 1000 / 60;

    private OpenGLWindow window;
    private Model model;
    private ModelRenderer renderer;
    private float lastLogicUpdateTimer;
    private float lastFrameUpdateTimer;

    public void init() {
        window = new OpenGLWindow(600, 600);
    }

    private void loadModel() {
        ModelLoader loader = new ModelLoader();
        model = loader.load("ship", MODEL_FILE);
    }

    public void mainLoop() {
        GL.createCapabilities();
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glCullFace(GL46.GL_BACK);
        GL46.glClearColor(0f, 0f, 0f, 0f);

        loadModel();

        renderer = new ModelRenderer();

        // rendering loop
        while (!window.shouldClose()) {
            if (shouldRunLogic()) {
                lastLogicUpdateTimer = System.currentTimeMillis();
            }
            if (shouldRender()) {
                render();
                window.update();
                lastFrameUpdateTimer = System.currentTimeMillis();
            }
            window.pollEvents();
        }
        cleanup();
    }

    private boolean shouldRender() {
        long now = System.currentTimeMillis();
        float deltaFrameTimer = (now - lastFrameUpdateTimer) / DEFAULT_FRAMES_PER_SECOND;
        return (deltaFrameTimer >= 1.0f);
    }

    private boolean shouldRunLogic() {
        long now = System.currentTimeMillis();
        float deltaLogicTimer = (now - lastLogicUpdateTimer) / LOGIC_UPDATES_PER_SECOND;
        return (deltaLogicTimer >= 1.0f);
    }

    private void render() {
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT); // clear framebuffers
        GL46.glViewport(0, 0, window.getWidth(), window.getHeight());
        // GL46.glPushMatrix();
        // GL46.glRotatef(model.getCurrentRotation(), 0f, 1f, 0f);
        renderer.render(model);
        // GL46.glPopMatrix();
    }

    private void cleanup() {
        window.cleanup();
    }

}
