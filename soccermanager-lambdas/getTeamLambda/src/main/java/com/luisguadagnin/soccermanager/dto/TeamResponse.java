package com.luisguadagnin.soccermanager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamResponse {

    private String id;
    private String name;
    private String country;
    private String value;
    private String budget;
    private List<PlayerResponse> players;

}
