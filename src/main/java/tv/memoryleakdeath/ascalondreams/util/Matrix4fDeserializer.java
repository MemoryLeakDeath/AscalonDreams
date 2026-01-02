package tv.memoryleakdeath.ascalondreams.util;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class Matrix4fDeserializer extends StdDeserializer<Matrix4f> {

   public Matrix4fDeserializer() {
      super(Matrix4f.class);
   }

   @Override
   public Matrix4f deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
      if(p.currentToken() == null) {
         p.nextToken();
      }
      if(p.currentToken() != JsonToken.START_ARRAY) {
         ctxt.reportInputMismatch(Matrix4f.class, "Expected START_ARRAY for Matrix4f deserialization!");
      }

      float[] values = new float[16];
      for(int i = 0; i < 16; i++) {
         JsonToken token = p.nextToken();
         if(token == null || !token.isNumeric()) {
            ctxt.reportInputMismatch(Matrix4f.class, "Expected numeric value at index: %d", i);
         }
         values[i] = p.getFloatValue();
      }

      JsonToken endToken = p.nextToken();
      if(endToken != JsonToken.END_ARRAY) {
         ctxt.reportInputMismatch(Matrix4f.class, "Expected END_ARRAY after 16 elements");
      }

      Matrix4f result = new Matrix4f();
      int valueIndex = 0;
      for(int row = 0; row < 4; row++) {
         Vector4f rowValues = new Vector4f()
                 .set(values[valueIndex], values[valueIndex+1],
                         values[valueIndex+2], values[valueIndex+3]);
         result.setRow(row, rowValues);
         valueIndex += 4;
      }

      return result;
   }
}
