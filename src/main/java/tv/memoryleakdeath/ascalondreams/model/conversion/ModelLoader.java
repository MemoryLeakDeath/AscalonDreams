package tv.memoryleakdeath.ascalondreams.model.conversion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tv.memoryleakdeath.ascalondreams.util.ObjectMapperInstance;

import java.io.File;

public class ModelLoader {
   private static final Logger logger = LoggerFactory.getLogger(ModelLoader.class);

   private ModelLoader() {

   }

   public static ConvertedModel loadModel(String modelFile) {
      logger.debug("Loading model file: {}", modelFile);
      ConvertedModel model = null;
      try {
         ObjectMapper mapper = ObjectMapperInstance.getInstance();
         File inputFile = new File(modelFile);
         model = mapper.readValue(inputFile, ConvertedModel.class);
      } catch (Exception e) {
         throw new RuntimeException("Unable to load model file: %s".formatted(modelFile), e);
      }
      return model;
   }
}
