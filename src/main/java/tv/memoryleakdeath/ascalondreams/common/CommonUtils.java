package tv.memoryleakdeath.ascalondreams.common;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class CommonUtils {
   private CommonUtils() {
   }

   public static <T> Consumer<T> withIndex(BiConsumer<Integer, T> consumer) {
      AtomicInteger index = new AtomicInteger(0);
      return item -> consumer.accept(index.getAndIncrement(), item);
   }
}
