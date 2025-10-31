package com.auth.authservice.google;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CodeStore {
    private final Map<String, String> codeTokenMap = new ConcurrentHashMap<>();
    private final Map<String, Long> expiryMap = new ConcurrentHashMap<>();

    public void saveCode(String type, String code, String token) {
        String key = type + ":" + code;
        codeTokenMap.put(key, token);
        expiryMap.put(key, System.currentTimeMillis() + 60000);
    }

    public String getToken(String type, String code) {
        String key = type + ":" + code;
        if (expiryMap.containsKey(key) && System.currentTimeMillis() < expiryMap.get(key)) {
            expiryMap.remove(key);
            return codeTokenMap.remove(key);
        }
        return null;
    }
}
