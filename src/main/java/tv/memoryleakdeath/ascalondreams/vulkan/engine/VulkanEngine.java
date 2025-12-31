package tv.memoryleakdeath.ascalondreams.vulkan.engine;

import imgui.ImGui;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.AL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.camera.Camera;
import tv.memoryleakdeath.ascalondreams.camera.CameraInputCallback;
import tv.memoryleakdeath.ascalondreams.gui.GuiInputCallback;
import tv.memoryleakdeath.ascalondreams.gui.GuiTexture;
import tv.memoryleakdeath.ascalondreams.input.InputTimer;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallback;
import tv.memoryleakdeath.ascalondreams.input.KeyboardCallbackHandler;
import tv.memoryleakdeath.ascalondreams.input.MouseCallbackHandler;
import tv.memoryleakdeath.ascalondreams.lighting.Light;
import tv.memoryleakdeath.ascalondreams.lighting.LightingInputCallback;
import tv.memoryleakdeath.ascalondreams.sound.SoundBuffer;
import tv.memoryleakdeath.ascalondreams.sound.SoundListener;
import tv.memoryleakdeath.ascalondreams.sound.SoundManager;
import tv.memoryleakdeath.ascalondreams.sound.SoundSource;
import tv.memoryleakdeath.ascalondreams.state.GameState;
import tv.memoryleakdeath.ascalondreams.state.StateMachine;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ConvertedModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ModelLoader;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.render.VulkanRenderer;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.Entity;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.scene.VulkanScene;

import java.util.ArrayList;
import java.util.List;

public class VulkanEngine {
    private static final Logger logger = LoggerFactory.getLogger(VulkanEngine.class);
    public static final String MODEL_FILE = "/home/mem/development/models/scifi-ship/FBX/ship.fbx";
    private static final String CUBE_MODEL_FILE = "models/cube/cube.json";
    private static final String SPONZA_MODEL_FILE = "models/sponza/Sponza.json";
    private static final String TREE_MODEL_FILE = "models/tree/tree.json";
    private static final int LOGIC_UPDATES_PER_SECOND = 30;
    private static final int DEFAULT_FRAMES_PER_SECOND = 60;
    private static final long LOGIC_FRAME_TIME = 1_000_000_000L / LOGIC_UPDATES_PER_SECOND;
    private static final long FPS_FRAME_TIME = 1_000_000_000L / DEFAULT_FRAMES_PER_SECOND;
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.01f;
    private final Vector3f rotationAngle = new Vector3f(1,1,1);
    private float angle = 0;

    private static final String SOUND_BUFFER_MUSIC = "music-sound-buffer";
    private static final String SOUND_BUFFER_PLAYER = "player-sound-buffer";
    private static final String SOUND_SOURCE_MUSIC = "music-sound-source";
    private static final String SOUND_SOURCE_PLAYER = "player-sound-source";
    private static final String SOUND_FILE1 = "sounds/creak1.ogg";
    private static final String SOUND_FILE2 = "sounds/woo_scary.ogg";

    private long soundTimer = 0;
    private long lightTimer = 0;

    private VulkanWindow window;
    private VulkanRenderer renderer;
    private long lastLogicUpdateTimer;
    private long lastFrameUpdateTimer;
    private KeyboardCallback kb;
    private VulkanScene scene;
    private Entity cubeEntity;
    private Entity sponzaEntity;
    private GuiTexture guiTexture;
    private SoundManager soundManager = SoundManager.getInstance();
    private Vector3f lastDirectionalLightPosition = new Vector3f();

    public void init() {
        window = new VulkanWindow(600, 600);
        this.scene = new VulkanScene(window);
        this.guiTexture = new GuiTexture("textures/vulkan.png");
        renderer = new VulkanRenderer(window, scene);
        renderer.initModels(
                List.of(loadModel(SPONZA_MODEL_FILE, "SponzaEntity", new Vector3f(0f, 0f, 0f)),
                        loadModel(TREE_MODEL_FILE, "TreeEntity", new Vector3f(0f, 0f, 0f), 0.005f)),
                List.of(guiTexture));
        initSceneLighting();
        setCameraStartState();
        initSound();
    }

    private void initSceneLighting() {
//       Entity lightEntity = scene.getEntities().get(1);
//       lightEntity.setScale(0.1f);
       scene.getAmbientLightColor().set(1f, 1f, 1f);
       scene.setAmbientLightIntensity(0.2f);

       List<Light> lights = new ArrayList<>();
       // directional light
       lights.add(new Light(new Vector3f(0f, -1f, 0f), true, 8.0f, new Vector3f(1f, 1f, 1f)));
       scene.setLights(lights);
    }

    private void initSound() {
       Camera camera = scene.getCamera();
       soundManager.setAttenuationModel(AL11.AL_EXPONENT_DISTANCE);
       soundManager.setListener(new SoundListener(camera.getPosition()));

       SoundBuffer buffer = new SoundBuffer(SOUND_FILE1);
       soundManager.addSoundBuffer(SOUND_BUFFER_PLAYER, buffer);
       SoundSource playerSoundSource = new SoundSource(false, false);
       playerSoundSource.setPosition(new Vector3f(0f,0f,0f));
       playerSoundSource.setBuffer(buffer.getId());
       soundManager.addSoundSource(SOUND_SOURCE_PLAYER, playerSoundSource);

       SoundBuffer musicBuffer = new SoundBuffer(SOUND_FILE2);
       soundManager.addSoundBuffer(SOUND_BUFFER_MUSIC, musicBuffer);
       SoundSource musicSource = new SoundSource(true, true);
       musicSource.setBuffer(musicBuffer.getId());
       soundManager.addSoundSource(SOUND_SOURCE_MUSIC, musicSource);
       musicSource.play();
    }

    private ConvertedModel loadModel(String modelFile, String entityId, Vector3f entityStartingPosition) {
       ConvertedModel convertedModel = ModelLoader.loadModel(modelFile);
       scene.addEntity(new Entity(entityId, convertedModel.getId(), entityStartingPosition));
       return convertedModel;
    }

    private ConvertedModel loadModel(String modelFile, String entityId, Vector3f entityStartingPosition, float scale) {
      ConvertedModel convertedModel = ModelLoader.loadModel(modelFile);
      Entity entity = new Entity(entityId, convertedModel.getId(), entityStartingPosition);
      entity.setScale(scale);
      scene.addEntity(entity);
      return convertedModel;
   }

    private void setCameraStartState() {
       Camera camera = scene.getCamera();
       camera.setPosition(-5f, 5f, 0f);
       camera.setRotation((float) Math.toRadians(20f), (float) Math.toRadians(90f));
    }

    public void mainLoop() {
       InputTimer.getInstance().tick();
       registerInputCallbacks();

        // rendering loop
       soundTimer = System.currentTimeMillis();
       while (!window.shouldClose()) {
          window.pollEvents();
          handleGui();
           if (shouldRunLogic()) {
              gameLogic();
           }
           if (shouldRender()) {
              render();
              window.update();
           }
           InputTimer.getInstance().tick();
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
       long timerDiff = System.currentTimeMillis() - soundTimer;
       if(timerDiff > 5000) {
          soundManager.play(SOUND_SOURCE_PLAYER, SOUND_BUFFER_PLAYER);
          soundTimer = System.currentTimeMillis();
       }
    }

    private void cleanup() {
       renderer.cleanup();
       window.cleanup();
    }

    private void registerInputCallbacks() {
       var keyHandler = KeyboardCallbackHandler.getInstance(window.getHandle());
       var mouseHandler = MouseCallbackHandler.getInstance(window.getHandle());
       var mouseEnteredHandler = mouseHandler.getEnteredHandler();
       var mouseButtonHandler = mouseHandler.getButtonHandler();
       var cameraInputCallback = new CameraInputCallback(scene.getCamera());
       var guiInputCallback = new GuiInputCallback();
       var lightingInputCallback = new LightingInputCallback(scene.getLights().getFirst());
       keyHandler.registerCallback(cameraInputCallback).registerCallback(guiInputCallback).registerCallback(lightingInputCallback);
       mouseHandler.registerCallback(cameraInputCallback).registerCallback(guiInputCallback);
       GLFW.glfwSetKeyCallback(window.getHandle(), keyHandler);
       GLFW.glfwSetCursorEnterCallback(window.getHandle(), mouseEnteredHandler);
       GLFW.glfwSetCursorPosCallback(window.getHandle(), mouseHandler);
       GLFW.glfwSetMouseButtonCallback(window.getHandle(), mouseButtonHandler);
    }

    private void handleGui() {
       if(StateMachine.getInstance().getCurrentGameState() == GameState.GUI) {
          ImGui.newFrame();
          ImGui.showDemoWindow();
          ImGui.endFrame();
          ImGui.render();
       }
    }
}
