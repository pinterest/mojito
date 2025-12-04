package com.box.l10n.mojito.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.Test;

public class StreamUtilsTest {
  static class Person {
    String name;
    int age;

    Person(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public String getName() {
      return name;
    }

    public int getAge() {
      return age;
    }

    // Defined to use test assertions on raw objects
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Person person = (Person) o;
      return age == person.age && Objects.equals(name, person.name);
    }
  }

  @Test
  public void testDistinctByKey_shouldRemoveDuplicateKeys() {
    List<Person> people =
        Arrays.asList(
            new Person("Alice", 30),
            new Person("Bob", 25),
            new Person("Alice", 35),
            new Person("Charlie", 40));
    List<Person> distinctByName =
        people.stream().filter(StreamUtils.distinctByKey(Person::getName)).toList();
    assertEquals(3, distinctByName.size());
    assertTrue(distinctByName.contains(new Person("Alice", 30)));
    assertTrue(distinctByName.contains(new Person("Bob", 25)));
    assertTrue(distinctByName.contains(new Person("Charlie", 40)));
  }

  @Test
  public void testDistinctByKey_shouldHandleEmptyLists() {
    List<Person> people = Collections.emptyList();
    List<Person> result =
        people.stream().filter(StreamUtils.distinctByKey(Person::getName)).toList();
    assertTrue(result.isEmpty());
  }

  @Test
  public void testDistinctByKey_shouldHandleSingleElement() {
    List<Person> people = List.of(new Person("Alice", 30));
    List<Person> result =
        people.stream().filter(StreamUtils.distinctByKey(Person::getName)).toList();
    assertEquals(1, result.size());
    assertEquals(new Person("Alice", 30), result.get(0));
  }

  @Test
  public void testDistinctByKey_shouldHandleAllDuplicates() {
    List<Person> people =
        Arrays.asList(new Person("Alice", 30), new Person("Alice", 30), new Person("Alice", 30));
    List<Person> result =
        people.stream().filter(StreamUtils.distinctByKey(Person::getName)).toList();
    assertEquals(1, result.size());
    assertEquals(new Person("Alice", 30), result.getFirst());
  }

  @Test
  public void testDistinctByKey_shouldHandleAllUnique() {
    List<Person> people =
        Arrays.asList(new Person("Alice", 30), new Person("Bob", 25), new Person("Charlie", 40));
    List<Person> result =
        people.stream().filter(StreamUtils.distinctByKey(Person::getName)).toList();
    assertEquals(3, result.size());
  }

  @Test
  public void testDistinctByKey_shouldRemoveNullKeys() {
    List<Person> people =
        Arrays.asList(new Person(null, 30), new Person(null, 25), new Person("Alice", 35));
    List<Person> result =
        people.stream().filter(StreamUtils.distinctByKey(Person::getName)).toList();
    assertEquals(1, result.size());
    // Null key is removed
    assertFalse(result.contains(new Person(null, 30)));
    assertTrue(result.contains(new Person("Alice", 35)));
  }

  @Test
  public void testDistinctByKey_shouldWorkInAParallelStream() {
    List<Person> people =
        Arrays.asList(
            new Person("Alice", 30),
            new Person("Bob", 25),
            new Person("Alice", 35),
            new Person("Charlie", 40),
            new Person("Bob", 50));
    List<Person> distinctByName =
        people.parallelStream().filter(StreamUtils.distinctByKey(Person::getName)).toList();
    assertEquals(3, distinctByName.size());
    assertTrue(distinctByName.stream().anyMatch(p -> p.getName().equals("Alice")));
    assertTrue(distinctByName.stream().anyMatch(p -> p.getName().equals("Bob")));
    assertTrue(distinctByName.stream().anyMatch(p -> p.getName().equals("Charlie")));
  }

  @Test
  public void testDistinctByKey_shouldHandleDifferentObjectLambdas() {
    List<Person> people =
        Arrays.asList(new Person("Alice", 30), new Person("Bob", 30), new Person("Charlie", 40));
    // Distinct by age
    List<Person> distinctByAge =
        people.stream().filter(StreamUtils.distinctByKey(Person::getAge)).toList();
    assertEquals(2, distinctByAge.size());
    assertTrue(distinctByAge.contains(new Person("Alice", 30)));
    assertTrue(distinctByAge.contains(new Person("Charlie", 40)));
  }
}
