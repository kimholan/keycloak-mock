package com.tngtech.keycloakmock.impl.handler;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import com.tngtech.keycloakmock.impl.CustomClaims;
import com.tngtech.keycloakmock.impl.TokenGenerator;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomClaimsRoute implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(CustomClaimsRoute.class);

  private final CustomClaims customClaims;

  @Inject
  public CustomClaimsRoute(@Nonnull TokenGenerator tokenGenerator) {
    this.customClaims = tokenGenerator.getCustomClaims();
  }

  @Override
  public void handle(@Nonnull RoutingContext ctx) {
    var method = ctx.request().method().name();

    if ("GET".equals(method)) {
      handleGet(ctx);
    } else if ("POST".equals(method)) {
      handlePost(ctx);
    }
  }

  private void handleGet(@NonNull RoutingContext ctx) {
    ctx.response()
        .putHeader(CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
        .end(customClaims.toString());
  }

  private void handlePost(@NonNull RoutingContext ctx) {
    var request = ctx.request();
    request.bodyHandler(
        buffer -> {
          var jsonString = buffer.toString();
          LOG.info("[POST /custom-claims] {}", jsonString);

          var entries = new JsonObject(jsonString);
          var claims = entries.getJsonObject("claims");
          var claimsMap = Optional.ofNullable(claims).map(JsonObject::getMap).orElseGet(Map::of);
          var azp = Optional.ofNullable(claims).map(it-> it.getString("azp")).orElse(null);
          var sub = Optional.ofNullable(claims).map(it-> it.getString("sub")).orElse(null);

          if (azp == null || sub == null) {
            LOG.error("[POST /custom-claims] azp or sub is null: {}", jsonString);
            ctx.fail(400);
          } else {
            var claimsJsonObject = new JsonObject(claimsMap);
            var claimsJsonString = claimsJsonObject.toString();
            customClaims.add(azp, sub, claimsMap);
            ctx.response().end(claimsJsonString);
          }
        });
  }
}
