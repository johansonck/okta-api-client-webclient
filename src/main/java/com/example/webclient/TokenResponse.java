package com.example.webclient;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class TokenResponse {
  @JsonProperty("token_type")
  String tokenType;
  @JsonProperty("expires_in")
  Integer expiresIn;
  @JsonProperty("access_token")
  String accessToken;
  String scope;
}
