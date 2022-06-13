package com.luisguadagnin.soccermanager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OfferResponse {

    private String id;
    private String price;
    private String discount;
    private PlayerResponse player;

}
