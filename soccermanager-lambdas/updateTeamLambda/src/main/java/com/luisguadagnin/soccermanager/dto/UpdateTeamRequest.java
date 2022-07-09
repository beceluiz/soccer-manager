package com.luisguadagnin.soccermanager.dto;

import lombok.Data;

@Data
public class UpdateTeamRequest {
    private String name;
    private String country;
}
