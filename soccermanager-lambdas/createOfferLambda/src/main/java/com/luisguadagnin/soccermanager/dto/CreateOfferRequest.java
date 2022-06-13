package com.luisguadagnin.soccermanager.dto;

import lombok.Data;

@Data
public class CreateOfferRequest {
    private String playerId;
    private String price;
}
