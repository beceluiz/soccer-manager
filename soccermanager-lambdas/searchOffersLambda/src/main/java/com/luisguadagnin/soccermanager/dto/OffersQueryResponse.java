package com.luisguadagnin.soccermanager.dto;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.luisguadagnin.soccermanager.model.Offer;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OffersQueryResponse {

    private List<Offer> offers;
    private Map<String, AttributeValue> lastEvaluatedKey;

}
