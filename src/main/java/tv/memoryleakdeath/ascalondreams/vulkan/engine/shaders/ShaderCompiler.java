package tv.memoryleakdeath.ascalondreams.vulkan.engine.shaders;

import org.apache.commons.io.FileUtils;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ShaderCompiler {
   private ShaderCompiler() {
   }

   public static byte[] compileShader(String code, int type, boolean debugShaders) {
      long compilerHandle = 0;
      long compilerOptionsHandle = 0;
      byte[] compiledShaderCode;

      try {
         compilerHandle = Shaderc.shaderc_compiler_initialize();
         compilerOptionsHandle = Shaderc.shaderc_compile_options_initialize();
         if(debugShaders) {
            Shaderc.shaderc_compile_options_set_generate_debug_info(compilerOptionsHandle);
            Shaderc.shaderc_compile_options_set_optimization_level(compilerOptionsHandle, 0);
            Shaderc.shaderc_compile_options_set_source_language(compilerOptionsHandle, Shaderc.shaderc_source_language_glsl);
         }

         long resultStatus = Shaderc.shaderc_compile_into_spv(compilerHandle, code, type, "shader.glsl", "main", compilerOptionsHandle);
         if(Shaderc.shaderc_result_get_compilation_status(resultStatus) != Shaderc.shaderc_compilation_status_success) {
            throw new RuntimeException("Shader compilation failed: %s".formatted(Shaderc.shaderc_result_get_error_message(resultStatus)));
         }

         ByteBuffer buf = Shaderc.shaderc_result_get_bytes(resultStatus);
         compiledShaderCode = new byte[buf.remaining()];
         buf.get(compiledShaderCode);
      } finally {
         Shaderc.shaderc_compile_options_release(compilerOptionsHandle);
         Shaderc.shaderc_compiler_release(compilerHandle);
      }
      return compiledShaderCode;
   }

   public static void compileShaderIfChanged(String glsShaderFile, int type, boolean debugShaders) {
      byte[] compiledShaderCode;
      try {
         var glslFile = new File(glsShaderFile);
         var spvFile = new File(glsShaderFile + ".spv");
         if(!spvFile.exists() || FileUtils.isFileNewer(glslFile, spvFile)) {
            var shaderCode = Files.readString(glslFile.toPath());
            compiledShaderCode = compileShader(shaderCode, type, debugShaders);
            Files.write(spvFile.toPath(), compiledShaderCode);
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
