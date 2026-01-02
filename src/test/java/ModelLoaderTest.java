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
         ModelConverter.convertModelFiles("models/sponza/Sponza.gltf", null);
         ModelConverter.convertModelFiles("models/cube/cube.obj", null);
         ModelConverter.convertModelFiles("models/tree/tree.obj", null);
         ModelConverter.convertModelFiles("models/bob/boblamp.md5mesh","models/bob/boblamp.md5anim");
      } catch (Exception e) {
         Assertions.fail("Failed with Exception!", e);
      }
   }

   @Test
   public void testLoadModel() {
      ConvertedModel model = ModelLoader.loadModel("models/sponza/Sponza.json");
      ConvertedModel model2 = ModelLoader.loadModel("models/cube/cube.json");
      ConvertedModel model3 = ModelLoader.loadModel("models/tree/tree.json");
      ConvertedModel model4 = ModelLoader.loadModel("models/bob/boblamp.json");
      System.out.println(model);
      System.out.println(model2);
      System.out.println(model3);
   }
}
