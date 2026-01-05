package tv.memoryleakdeath.ascalondreams.util;

import org.joml.Matrix4f;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.dataformat.smile.SmileMapper;

public class ObjectMapperInstance {
   private static SmileMapper mapper;

   private ObjectMapperInstance() {
   }

   public static SmileMapper getInstance() {
      if(mapper == null) {
         mapper = SmileMapper.builder()
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
