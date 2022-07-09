package com.luisguadagnin.soccermanager.dto;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SearchOffersRequest {
    private String country;
    private PlayerPosition position;
    private String orderBy;
    private String orderDirection;
    private int pageSize;
    private Map<String, AttributeValue> exclusiveStartKey;
}
