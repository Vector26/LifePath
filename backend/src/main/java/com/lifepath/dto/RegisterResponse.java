package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RegisterResponse {

    private UserResponse user;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    public RegisterResponse(UserResponse user, String accessToken, String refreshToken, int expiresIn) {
        this.user = user;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public UserResponse getUser() { return user; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public int getExpiresIn() { return expiresIn; }
}
