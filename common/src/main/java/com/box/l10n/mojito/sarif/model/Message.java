package com.box.l10n.mojito.sarif.model;

public class Message {
  private String text;
  private String markdown;

  public Message(String text, String markdown) {
    this.text = text;
    this.markdown = markdown;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
  }
}
