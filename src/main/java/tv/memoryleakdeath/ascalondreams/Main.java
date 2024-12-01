package tv.memoryleakdeath.ascalondreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.opengl.engine.OpenGLEngine;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private void run() {
        logger.info("Starting program....");
        OpenGLEngine engine = new OpenGLEngine();
        engine.init();
        engine.mainLoop();
        logger.info("Exiting program....");
    }

//    private KeyboardCallback registerKeyboardCallbacks() {
//        KeyboardCallback kb = new KeyboardCallback();
//        kb.addHandler(GLFW.GLFW_KEY_ESCAPE, new UserInputCallback() {
//            @Override
//            public void performAction(int action) {
//                if (action == GLFW.GLFW_RELEASE) {
//                    GLFW.glfwSetWindowShouldClose(windowHandle, true);
//                }
//            }
//        }).addHandler(GLFW.GLFW_KEY_RIGHT, new UserInputCallback() {
//            @Override
//            public void performAction(int action) {
//                if (action == GLFW.GLFW_PRESS) {
//                    // model.setCurrentRotation(model.getCurrentRotation() + 0.08f);
//                }
//            }
//        }).addHandler(GLFW.GLFW_KEY_LEFT, new UserInputCallback() {
//            @Override
//            public void performAction(int action) {
//                if (action == GLFW.GLFW_PRESS) {
//                    // model.setCurrentRotation(model.getCurrentRotation() - 0.08f);
//                }
//            }
//        });
//        return kb;
//    }

    public static void main(String[] args) {
        new Main().run();
    }

}
