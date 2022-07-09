package com.luisguadagnin.soccermanager.dto;

import lombok.Data;

@Data
public class CreateUserRequest {

    private String email;
    private String password;

}