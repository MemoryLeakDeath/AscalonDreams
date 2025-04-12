package tv.memoryleakdeath.ascalondreams.common.shaders;

import org.lwjgl.util.shaderc.Shaderc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public final class ShaderCompiler {
   private ShaderCompiler() {
   }

   public static byte[] compile(String code, int shaderType, String shaderName) {
      long compiler = 0;
      long options = 0;
      byte[] compiledShader;
      try {
         compiler = Shaderc.shaderc_compiler_initialize();
         options = Shaderc.shaderc_compile_options_initialize();

         long result = Shaderc.shaderc_compile_into_spv(compiler, code, shaderType, shaderName, "main", options);
         if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
            throw new RuntimeException("Shader compilation failed!: %s".formatted(Shaderc.shaderc_result_get_error_message(result)));
         }

         ByteBuffer buffer = Shaderc.shaderc_result_get_bytes(result);
         compiledShader = new byte[buffer.remaining()];
         buffer.get(compiledShader);
      } finally {
         Shaderc.shaderc_compile_options_release(options);
         Shaderc.shaderc_compiler_release(compiler);
      }
      return compiledShader;
   }

   public static void compileIfModified(String glslShaderFileName, int shaderType) {
      try {
         File glslFile = new File(glslShaderFileName);
         File spvFile = new File(glslShaderFileName + ".spv");
         if (!spvFile.exists() || glslFile.lastModified() > spvFile.lastModified()) {
            String shaderCode = new String(Files.readAllBytes(glslFile.toPath()));
            byte[] compiledShader = compile(shaderCode, shaderType, glslShaderFileName);
            Files.write(spvFile.toPath(), compiledShader);
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
