package com.luisguadagnin.soccermanager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchOffersResponse {

    private KeyResource lastEvaluatedKey;
    private List<OfferResponse> offers;

}
