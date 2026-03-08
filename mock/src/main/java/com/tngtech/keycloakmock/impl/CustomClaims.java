package com.tngtech.keycloakmock.impl;

import static java.util.concurrent.TimeUnit.MINUTES;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomClaims {

  private static final Logger LOG = LoggerFactory.getLogger(CustomClaims.class);

  private static final Cache<String, Map<String, Object>> CACHED = newCache();
  private static final Map<String, Map<String, Object>> HARDCODED = newHardcoded();

  public CustomClaims() {
    // NOOP
  }

  public CustomClaims(Map<String, Map<String, Object>> hardcoded) {
    HARDCODED.putAll(hardcoded);
  }

  public Map<String, Object> get(@NonNull String azp, @NonNull String sub) {
    var key = key(azp, sub);

    var hardcoded = HARDCODED.get(key);
    if (hardcoded != null) {
      LOG.info("[CUSTOM_CLAIMS::GET_HARDCODED] ({},{})={}", azp, sub, hardcoded);
      return hardcoded;
    }

    var cached = CACHED.getIfPresent(key);
    if (cached != null) {
      LOG.info("[CUSTOM_CLAIMS::GET_CACHED] ({},{})={}", azp, sub, cached);
      return cached;
    }

    LOG.info("[CUSTOM_CLAIMS::GET_MISS] {}", key);

    return null;
  }

  public void add(@NonNull String azp, @NonNull String sub, @NonNull Map<String, Object> map) {
    LOG.info("[CUSTOM_CLAIMS::ADD_CACHED] ({},{})={}", azp, sub, map);
    CACHED.put(key(azp, sub), map);
  }

  public void hardcode(@NonNull String azp, @NonNull String sub, @NonNull Map<String, Object> map) {
    LOG.info("[CUSTOM_CLAIMS::ADD_HARDCODED] ({},{})={}", azp, sub, map);
    HARDCODED.put(key(azp, sub), map);
  }

  @Override
  public String toString() {
    var map = new HashMap<String, Object>();
    map.putAll(CACHED.asMap());
    map.putAll(HARDCODED);
    return new JsonObject(map).toString();
  }

  private static @NonNull String key(@NonNull String azp, @NonNull String sub) {
    return azp + "|" + sub;
  }

  private static Cache<String, Map<String, Object>> newCache() {
    return Caffeine.newBuilder().expireAfterAccess(2, MINUTES).expireAfterWrite(2, MINUTES).build();
  }

  private static Map<String, Map<String, Object>> newHardcoded() {
    return new ConcurrentHashMap<>(100, 0.5f);
  }
}
