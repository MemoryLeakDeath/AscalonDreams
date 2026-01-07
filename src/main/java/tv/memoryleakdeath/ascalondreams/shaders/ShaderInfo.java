package tv.memoryleakdeath.ascalondreams.shaders;

public record ShaderInfo(String file, int type, boolean debug) {
   public String getSpvName() {
      return file + ".spv";
   }
}
