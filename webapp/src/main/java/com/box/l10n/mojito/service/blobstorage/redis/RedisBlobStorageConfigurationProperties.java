package com.box.l10n.mojito.service.blobstorage.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties("l10n.blob-storage.redis")
public class RedisBlobStorageConfigurationProperties {

    private String hostname;

    private int port;

    private int clientTimeoutInSeconds = 60;

    private Set<String> cacheKeyPrefixes = new HashSet<>();

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

    public Set<String> getCacheKeyPrefixes() {
        return cacheKeyPrefixes;
    }

    public void setCacheKeyPrefixes(Set<String> cacheKeyPrefixes) {
        this.cacheKeyPrefixes = cacheKeyPrefixes;
    }
}
