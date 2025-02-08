package tv.memoryleakdeath.ascalondreams;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
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
        CommandArgs cmdArgs = parseCommandLine(args);
        if (CommandArgs.USE_VULKAN.equalsIgnoreCase(cmdArgs.getUseEngine())) {
            new Main().runVulkan();
        } else {
            new Main().runOpenGL();
        }
    }

    private static CommandArgs parseCommandLine(String[] args) {
        CommandArgs cmd = new CommandArgs();
        CmdLineParser parser = new CmdLineParser(cmd);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            logger.error("Unknown command line options!", e);
            System.out.println("Unknown command line option(s), using defaults!");
            parser.printUsage(System.out);
        }
        return cmd;
    }

}

class CommandArgs {
    public static final String USE_OPENGL = "opengl";
    public static final String USE_VULKAN = "vulkan";

    @Option(name = "-e", usage = "What engine to use [opengl,vulkan]", metaVar = USE_OPENGL)
    private String useEngine = USE_OPENGL;

    public String getUseEngine() {
        return useEngine;
    }
}
