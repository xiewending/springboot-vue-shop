package com.shoppilot.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuVO> menus;
}
