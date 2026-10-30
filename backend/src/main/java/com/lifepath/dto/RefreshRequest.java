package com.lifepath.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RefreshRequest {

    @JsonAlias("refresh_token")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
