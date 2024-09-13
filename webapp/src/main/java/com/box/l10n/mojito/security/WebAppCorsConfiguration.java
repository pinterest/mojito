package com.box.l10n.mojito.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class WebAppCorsConfiguration {

    @Value("${security.cors.allowed.origins:}")
    private String[] origins;
    @Value("${security.cors.allowed.methods:}")
    private String[] methods;
    @Value("${security.cors.allowed.headers:}")
    private String[] headers;

    public List<String> getOrigins() {
        return Arrays.asList(origins);
    }

    public List<String> getMethods() {
        return Arrays.asList(methods);
    }

    public List<String> getHeaders() {
        return Arrays.asList(headers);
    }
}
