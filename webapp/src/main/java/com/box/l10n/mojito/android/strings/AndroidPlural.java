package com.box.l10n.mojito.android.strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AndroidPlural extends AbstractAndroidString {

  private final Map<AndroidPluralQuantity, AndroidPluralItem> items;

  public static AndroidPluralBuilder builder() {
    return new AndroidPluralBuilder();
  }

  public AndroidPlural(String name, String comment, List<AndroidPluralItem> items) {
    super(name, comment);
    this.items = buildItemMap(items);
  }

  public Map<AndroidPluralQuantity, AndroidPluralItem> getItems() {
    return items;
  }

  public void forEachItemSorted(Consumer<AndroidPluralItem> itemConsumer) {
    sortedStream().forEach(itemConsumer);
  }

  public Stream<AndroidPluralItem> sortedStream() {
    return items.values().stream()
        .sorted(Comparator.comparingInt(item -> item.getQuantity().ordinal()));
  }

  private Map<AndroidPluralQuantity, AndroidPluralItem> buildItemMap(
      List<AndroidPluralItem> items) {
    return items.stream()
        .collect(
            Collectors.toMap(
                AndroidPluralItem::getQuantity,
                Function.identity(),
                (androidPluralItem, androidPluralItem2) -> {
                  throw new AndroidPluralDuplicateKeyException(
                      "A duplicate was found when building an Android Plural. "
                          + androidPluralItem
                          + " dupplicates: "
                          + androidPluralItem2);
                }));
  }

  @Override
  public boolean isSingular() {
    return false;
  }

  public static final class AndroidPluralBuilder {

    private List<AndroidPluralItem> items = new ArrayList<>();
    private String comment;
    private String name;

    public AndroidPluralBuilder() {}

    public AndroidPluralBuilder addItem(AndroidPluralItem item) {
      items.add(item);
      return this;
    }

    public AndroidPluralBuilder setName(String name) {
      this.name = name;
      return this;
    }

    public AndroidPluralBuilder setComment(String comment) {
      this.comment = comment;
      return this;
    }

    public AndroidPlural build() {
      return new AndroidPlural(name, comment, items);
    }

    public boolean hasItemWithSameQuantityAndDifferentContent(AndroidPluralItem pluralItem) {
      return this.items.stream()
          .anyMatch(
              item ->
                  item.getQuantity().equals(pluralItem.getQuantity())
                      && !item.getContent().equals(pluralItem.getContent()));
    }

    public Set<AndroidPluralQuantity> getMissingQuantities() {
      Set<AndroidPluralQuantity> currentQuantities =
          this.items.stream().map(AndroidPluralItem::getQuantity).collect(Collectors.toSet());
      return Arrays.stream(AndroidPluralQuantity.values())
          .filter(quantity -> !currentQuantities.contains(quantity))
          .collect(Collectors.toSet());
    }

    public Optional<AndroidPluralItem> getItem(AndroidPluralQuantity quantity) {
      return this.items.stream()
          .filter(item -> item.getQuantity().equals(quantity))
          .map(item -> new AndroidPluralItem(item.getId(), item.getQuantity(), item.getContent()))
          .findFirst();
    }

    public int getItemsSize() {
      return this.items.size();
    }

    public String getName() {
      return this.name;
    }
  }
}
