package com.box.l10n.mojito.cli.command.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SarifUtilsTest {

  @Test
  void testRemoveFilePathPrefixNullPrefixReturnsSameMap() {
    Map<String, Set<Integer>> input = Map.of("foo/bar.txt", Set.of(3, 5));
    Map<String, Set<Integer>> result = SarifUtils.removeFilePathPrefix(input, null);
    assertSame(input, result);
  }

  @Test
  void testRemoveFilePathPrefixEmptyStringReturnsSameMap() {
    Map<String, Set<Integer>> input = Map.of("some/file.txt", Set.of(1));
    Map<String, Set<Integer>> result = SarifUtils.removeFilePathPrefix(input, "");
    assertSame(input, result);
  }

  @Test
  void testRemoveFilePathPrefixPrefixIsRemovedFromKeys() {
    Map<String, Set<Integer>> input =
        Map.of(
            "/prefix/path/file1.txt", Set.of(1, 2),
            "/prefix/path/file2.txt", Set.of(4));
    Map<String, Set<Integer>> expected =
        Map.of(
            "file1.txt", Set.of(1, 2),
            "file2.txt", Set.of(4));
    Map<String, Set<Integer>> result = SarifUtils.removeFilePathPrefix(input, "/prefix/path/");
    assertEquals(expected, result);
  }

  @Test
  void testRemoveFilePathPrefixWhenPrefixDoesNotMatchNot() {
    Map<String, Set<Integer>> input =
        Map.of(
            "foo/bar.txt", Set.of(12),
            "baz/qux.txt", Set.of(6));
    Map<String, Set<Integer>> result = SarifUtils.removeFilePathPrefix(input, "nonMatching/");
    assertEquals(input, result);
  }

  @Test
  void testRemoveFilePathPrefixWithPartialPrefix() {
    Map<String, Set<Integer>> input = Map.of("root/path1/file.txt", Set.of(8));
    // Only "root/path" not "root/path1"
    Map<String, Set<Integer>> result = SarifUtils.removeFilePathPrefix(input, "root/path/");
    assertEquals(input, result);
  }

  @Test
  void testRemoveFilePathPrefixValuesAreUnchanged() {
    Set<Integer> set = new HashSet<>(Arrays.asList(2, 4, 6));
    Map<String, Set<Integer>> input = Map.of("/p/test.txt", set);
    Map<String, Set<Integer>> result = SarifUtils.removeFilePathPrefix(input, "/p/");
    assertSame(set, result.get("test.txt"));
  }

  @Test
  void testRemoveFilePathPrefixSomeRemovedSomeNot() {
    Map<String, Set<Integer>> input =
        Map.of(
            "/pref/a.txt", Set.of(1),
            "/pref/b.txt", Set.of(2),
            "c.txt", Set.of(3));
    Map<String, Set<Integer>> expected =
        Map.of(
            "a.txt", Set.of(1),
            "b.txt", Set.of(2),
            "c.txt", Set.of(3));
    Map<String, Set<Integer>> result = SarifUtils.removeFilePathPrefix(input, "/pref/");
    assertEquals(expected, result);
  }
}
