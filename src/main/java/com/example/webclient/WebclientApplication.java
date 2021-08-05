package com.example.webclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@SpringBootApplication
@EnableScheduling
public class WebclientApplication implements ApplicationRunner {

  Logger logger = LoggerFactory.getLogger(CommandLineRunner.class);

  private final WebClient webClient = WebClient.builder().build();

  public static void main(String[] args) {
    SpringApplication.run(WebclientApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) {
    String oktaDomain = args.getOptionValues("okta-domain").stream().findFirst().orElseThrow();
    String clientId = args.getOptionValues("client-id").stream().findFirst().orElseThrow();

    String body = webClient.get()
        .uri("https://" + oktaDomain + "/api/v1/users?limit=25")
        .headers(h -> h.setBearerAuth(getAccessToken(oktaDomain, clientId)))
        .retrieve()
        .bodyToMono(String.class)
        .block();

    logger.info(body);
  }

  private String getAccessToken(String oktaDomain, String clientId) {
    return webClient.post()
        .uri("https://" + oktaDomain + "/oauth2/v1/token")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(
            BodyInserters.fromFormData("grant_type", "client_credentials")
                .with(
                    "client_assertion_type",
                    "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"
                )
                .with("scope", "okta.users.read")
                .with("client_assertion", createJwtToken(oktaDomain, clientId))
        )
        .retrieve()
        .bodyToMono(String.class)
        .map(this::toTokenResponse)
        .map(TokenResponse::getAccessToken)
        .block();
  }

  private TokenResponse toTokenResponse(String it) {
    try {
      return new ObjectMapper().readValue(it, TokenResponse.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private String createJwtToken(String oktaDomain, String clientId) {
    return Jwts.builder()
        .setAudience("https://" + oktaDomain + "/oauth2/v1/token")
        .setIssuer(clientId)
        .setSubject(clientId)
        .setExpiration(Date.from(Instant.now().plus(5L, ChronoUnit.MINUTES)))
        .signWith(SignatureAlgorithm.RS256, readPrivateKey())
        .compact();
  }

  private PrivateKey readPrivateKey() {
    try {
      try (Reader keyReader = getKeyReader()) {
        return new JcaPEMKeyConverter().getPrivateKey(
            PrivateKeyInfo.getInstance(new PEMParser(keyReader).readObject())
        );
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private Reader getKeyReader() {
    return new InputStreamReader(
        Objects.requireNonNull(getClass().getResourceAsStream("/privateKey.pem"))
    );
  }
}