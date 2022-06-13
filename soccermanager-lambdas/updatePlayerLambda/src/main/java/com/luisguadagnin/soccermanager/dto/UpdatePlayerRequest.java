package com.luisguadagnin.soccermanager.dto;

import lombok.Data;

@Data
public class UpdatePlayerRequest {
    private String firstName;
    private String lastName;
    private String country;
}
