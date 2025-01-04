package tv.memoryleakdeath.ascalondreams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tv.memoryleakdeath.ascalondreams.opengl.engine.OpenGLEngine;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.VulkanEngine;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private void runOpenGL() {
        logger.info("Starting program in OpenGL mode....");
        OpenGLEngine engine = new OpenGLEngine();
        engine.init();
        engine.mainLoop();
        logger.info("Exiting program....");
    }

    private void runVulkan() {
        logger.info("Starting program in Vulkan mode....");
        VulkanEngine engine = new VulkanEngine();
        engine.init();
        engine.mainLoop();
        logger.info("Exiting program....");
    }

    public static void main(String[] args) {
        // new Main().runOpenGL();
        new Main().runVulkan();
    }

}
