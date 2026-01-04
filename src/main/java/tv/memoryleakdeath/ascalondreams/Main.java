package tv.memoryleakdeath.ascalondreams;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tv.memoryleakdeath.ascalondreams.engine.VulkanEngine;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private void runVulkan() {
        logger.info("Starting program in Vulkan mode....");
        VulkanEngine engine = new VulkanEngine();
        engine.init();
        engine.mainLoop();
        logger.info("Exiting program....");
    }

//    private boolean convertModelFile(String filename) {
//       logger.info("Converting model file: {}", filename);
//       try {
//          ModelConverter.processFile(filename);
//       } catch (Exception e) {
//          logger.error("Unable to process model file: %s".formatted(filename), e);
//          return false;
//       }
//       return true;
//    }

    public static void main(String[] args) {
        CommandArgs cmdArgs = parseCommandLine(args);
        Main main = new Main();
        boolean modelConversionError = false;
//        if(StringUtils.isNotBlank(cmdArgs.getConvertModelFile())) {
//           if(!main.convertModelFile(cmdArgs.getConvertModelFile())) {
//              modelConversionError = true;
//           }
//        }
//        if(modelConversionError) {
//           System.out.println("Error converting model!  Exiting.");
//           return;
//        }
         main.runVulkan();
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
    @Option(name = "-m", usage = "Name of model file to convert")
    private String convertModelFile = "";

   public String getConvertModelFile() {
      return convertModelFile;
   }
}
