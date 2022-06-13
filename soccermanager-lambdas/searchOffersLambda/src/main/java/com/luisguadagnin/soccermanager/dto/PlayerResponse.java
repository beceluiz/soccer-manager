package com.luisguadagnin.soccermanager.dto;

import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String country;
    private int age;
    private String value;
    private PlayerPosition position;

}
