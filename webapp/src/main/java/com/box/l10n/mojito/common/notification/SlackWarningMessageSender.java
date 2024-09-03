package com.box.l10n.mojito.common.notification;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.HtmlTagIntegrityCheckerException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.slack.SlackClients;
import com.box.l10n.mojito.slack.request.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Sends warning messages to the specified warning channel on Slack.
 * Selects default Slack client specified in app properties, if none specified the first Slack Client is used.
 * The channel name for warnings must be placed in the app properties under: l10n.slackWarningChannel
 *
 * @author mattwilshire
 */
@Component
public class SlackWarningMessageSender {

    static Logger logger = LoggerFactory.getLogger(SlackWarningMessageSender.class);

    @Autowired SlackClients slackClients;

    @Value("${l10n.slackDefaultClient:#{null}}")
    private String defaultSlackClientId;

    // Channel to send warnings to, must be defined in application.properties
    @Value("${l10n.slackWarningChannel:#{null}}")
    private String slackWarningChannel;

    private SlackClient slackClient;

    @PostConstruct
    public void init() {
        // Use default slack client specified in app properties
        if (defaultSlackClientId != null) {
            this.slackClient = slackClients.getById(defaultSlackClientId);
        } else {
            // No default specified using first slack client
            for(Map.Entry<String, SlackClient> entry : slackClients.getMapIdToClient().entrySet()) {
                this.slackClient = entry.getValue();
                break;
            }
        }
    }

    public void handleIntegrityException(IntegrityCheckException exception, TMTextUnit tmTextUnit, String targetString) {
        try {
            // Branch off this if statement to send custom warning messages for custom integrity checkers
            if (exception instanceof HtmlTagIntegrityCheckerException) {
                sendWarning(SlackMessageBuilder.warnTagIntegrity(tmTextUnit, targetString));
            }
        } catch(SlackClientException ex) {
            logger.warn("Error sending message from slack client", ex);
        }
    }

    public void sendWarning(Message warning) throws SlackClientException {
        if(slackClient == null) {
            logger.warn("Attempted to send Slack warning but there was no slack client.");
            return;
        }
        if(slackWarningChannel == null) {
            logger.warn("Attempted to send Slack warning but there was no slack channel defined.");
            return;
        }

        warning.setChannel(slackWarningChannel);
        slackClient.sendInstantMessage(warning);
    }
}