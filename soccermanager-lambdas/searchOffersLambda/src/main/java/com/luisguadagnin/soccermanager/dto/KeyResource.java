package com.luisguadagnin.soccermanager.dto;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyResource {

    @JsonProperty("pk")
    private String partitionKeyName;

    @JsonProperty("pkv")
    private String partitionKeyValue;

    @JsonProperty("sk")
    private String sortKeyName;

    @JsonProperty("skv")
    private String sortKeyValue;

    @JsonProperty("id")
    private String offerId;

    public KeyResource(Map<String, AttributeValue> map) {
        this.offerId = map.get("id").getS();
        this.sortKeyName = map.keySet().stream().filter(key -> List.of("price", "discount").contains(key)).findFirst().orElseThrow();
        this.sortKeyValue = map.get(sortKeyName).getN();
        this.partitionKeyName = map.keySet().stream().filter(key -> List.of("sort_partition", "country", "position", "country_position").contains(key)).findFirst().orElseThrow();
        this.partitionKeyValue = "sort_partition".equals(partitionKeyName) ? map.get(partitionKeyName).getN() : map.get(partitionKeyName).getS();
    }

    public Map<String, AttributeValue> toMap() {
        AttributeValue partitionValue = new AttributeValue();
        if ("sort_partition".equals(partitionKeyName)) {
            partitionValue = partitionValue.withN(partitionKeyValue);
        } else {
            partitionValue = partitionValue.withS(partitionKeyValue);
        }

        return Map.of(
                partitionKeyName, partitionValue,
                sortKeyName, new AttributeValue().withN(sortKeyValue),
                "id", new AttributeValue().withS(offerId)
        );
    }


}
