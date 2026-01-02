package tv.memoryleakdeath.ascalondreams.util;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class Matrix4fSerializer extends StdSerializer<Matrix4f> {

   public Matrix4fSerializer() {
      super(Matrix4f.class);
   }

   @Override
   public void serialize(Matrix4f value, JsonGenerator gen, SerializationContext provider) throws JacksonException {
      double[] result = new double[16];
      int resultIndex = 0;
      for(int row = 0; row < 4; row++) {
         Vector4f rowValues = new Vector4f();
         value.getRow(row, rowValues);
         result[resultIndex] = rowValues.x();
         result[resultIndex + 1] = rowValues.y();
         result[resultIndex + 2] = rowValues.z();
         result[resultIndex + 3] = rowValues.w();
         resultIndex += 4;
      }
      gen.writeArray(result, 0, 16);
   }
}
