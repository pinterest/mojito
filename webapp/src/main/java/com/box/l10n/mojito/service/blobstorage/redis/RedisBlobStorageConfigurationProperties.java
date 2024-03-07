package com.box.l10n.mojito.service.blobstorage.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.blob-storage.redis")
public class RedisBlobStorageConfigurationProperties {

    private String hostname;

    private int port;

    private int clientTimeoutInSeconds;

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public int getClientTimeoutInSeconds() {
        return clientTimeoutInSeconds;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setClientTimeoutInSeconds(int clientTimeoutInSeconds) {
        this.clientTimeoutInSeconds = clientTimeoutInSeconds;
    }
}
