package com.box.l10n.mojito.stream;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class StreamUtils {

  /**
   * Keeps track of keys extracted from input elements, to filter stream so that only the first
   * occurrence of each key passes through.
   *
   * <p>Useful for filtering distinct elements by a specific property when processing a stream. The
   * predicate is thread-safe and can be used in parallel streams.
   *
   * <p>Note: null keys are filtered out.
   *
   * @param keyExtractor a function to extract the key used for uniqueness from each element
   * @param <T> the type of the input elements
   * @return a predicate that returns {@code true} if the element's key has not been seen before
   */
  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();

    return t -> {
      Object key = keyExtractor.apply(t);
      if (key == null) {
        return false;
      }

      return seen.add(key);
    };
  }
}
