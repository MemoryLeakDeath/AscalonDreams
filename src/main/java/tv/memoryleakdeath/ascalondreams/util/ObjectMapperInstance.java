package tv.memoryleakdeath.ascalondreams.util;

import org.joml.Matrix4f;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class ObjectMapperInstance {
   private static ObjectMapper mapper;

   private ObjectMapperInstance() {
   }

   public static ObjectMapper getInstance() {
      if(mapper == null) {
         mapper = JsonMapper.builder()
                 .addModule(buildModule())
                 .build();
      }
      return mapper;
   }

   private static SimpleModule buildModule() {
      SimpleModule mappingModules = new SimpleModule();
      mappingModules.addSerializer(Matrix4f.class, new Matrix4fSerializer());
      mappingModules.addDeserializer(Matrix4f.class, new Matrix4fDeserializer());
      return mappingModules;
   }
}
