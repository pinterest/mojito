package com.box.l10n.mojito.common.notification;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.slack.request.Attachment;
import com.box.l10n.mojito.slack.request.Field;
import com.box.l10n.mojito.slack.request.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.box.l10n.mojito.slack.SlackClient.COLOR_WARNING;

/**
 * Build custom Slack warning messages below.
 */
@Component
public class SlackMessageBuilder {

    static public Message warnTagIntegrity(TMTextUnit tmTextUnit, String targetString) {
        Message message = new Message();
        message.setText(""); // Needed to avoid missing text error

        Attachment attachment = new Attachment();
        attachment.setTitle("Tag Mismatch In Translated String");
        attachment.setText("The source and target string below have a mismatch in their tags.");

        List<Field> fields = new ArrayList<>();

        //  This shouldn't happen but just in case
        if(tmTextUnit != null) {
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

    static private Field createField(String title, String value) {
        Field field = new Field();
        field.setTitle(title);
        field.setValue(value);
        return field;
    }
}