import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ConvertedModel;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ModelConverter;
import tv.memoryleakdeath.ascalondreams.vulkan.engine.model.conversion.ModelLoader;

public class ModelLoaderTest {

   @BeforeAll
   public static void testProcessFile() {
      try {
         ModelConverter.processFile("models/sponza/Sponza.gltf", false);
         ModelConverter.processFile("models/cube/cube.obj", false);
         ModelConverter.processFile("models/tree/tree.obj", false);
      } catch (Exception e) {
         Assertions.fail("Failed with Exception!", e);
      }
   }

   @Test
   public void testLoadModel() {
      ConvertedModel model = ModelLoader.loadModel("models/sponza/Sponza.json");
      ConvertedModel model2 = ModelLoader.loadModel("models/cube/cube.json");
      ConvertedModel model3 = ModelLoader.loadModel("models/tree/tree.json");
      System.out.println(model);
      System.out.println(model2);
      System.out.println(model3);
   }
}
