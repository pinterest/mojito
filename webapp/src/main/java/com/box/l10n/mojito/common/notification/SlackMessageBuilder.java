package com.box.l10n.mojito.common.notification;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Field;
import com.box.l10n.mojito.slack.request.Message;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Build custom Slack warning messages below. */
@Component
@ConditionalOnProperty(value = "l10n.integrity-check-notifier.enabled", havingValue = "true")
public class SlackMessageBuilder {

  @Autowired IntegrityCheckNotifierConfiguration integrityCheckNotifierConfiguration;

  public Message warnTagIntegrity(
      IntegrityCheckException exception, TMTextUnit tmTextUnit, String targetString) {
    Message message = new Message();
    message.setText(""); // Needed to avoid missing text error

    String title = "";
    String text = "";

    // Make sure there is custom messages setup for this warning, if not use the exception name and
    // message.
    if (integrityCheckNotifierConfiguration.getWarnings() != null
        && !integrityCheckNotifierConfiguration.getWarnings().isEmpty()
        && integrityCheckNotifierConfiguration.getWarnings().get("html-tag") != null) {
      title = integrityCheckNotifierConfiguration.getWarnings().get("html-tag").getTitle();
      text = integrityCheckNotifierConfiguration.getWarnings().get("html-tag").getText();
    } else {
      title = exception.getClass().getSimpleName();
      text = exception.getMessage();
    }

    Attachment attachment = new Attachment();
    attachment.setTitle(title);
    attachment.setText(text);

    List<Field> fields = new ArrayList<>();

    // This shouldn't ever be null, just in case something goes wrong send the target string only.
    if (tmTextUnit != null) {
      fields.add(createField("Repo", tmTextUnit.getAsset().getRepository().getName()));
      fields.add(createField("Text Unit Id", tmTextUnit.getId().toString()));
      fields.add(createField("Source", tmTextUnit.getContent()));
    }

    fields.add(createField("Target", targetString));

    attachment.setFields(fields);
    attachment.setColor(COLOR_WARNING);

    message.getAttachments().add(attachment);

    return message;
  }

  private Field createField(String title, String value) {
    Field field = new Field();
    field.setTitle(title);
    field.setValue(value);
    return field;
  }
}
