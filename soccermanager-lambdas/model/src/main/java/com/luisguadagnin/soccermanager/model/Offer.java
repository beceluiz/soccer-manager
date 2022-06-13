package com.luisguadagnin.soccermanager.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@DynamoDBTable(tableName = "Offer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "price")
    private long price;

    @DynamoDBAttribute(attributeName = "discount")
    private int discount;

    @DynamoDBAttribute(attributeName = "country")
    private String country;

    @DynamoDBTypeConvertedEnum
    @DynamoDBAttribute(attributeName = "position")
    private PlayerPosition position;

    @DynamoDBAttribute(attributeName = "country_position")
    private String countryPosition;

    @DynamoDBAttribute(attributeName = "sort_partition")
    private int sortPartition;

    @Builder
    public Offer(String id, long price, int discount, String country, PlayerPosition position) {
        this.id = id;
        this.price = price;
        this.discount = discount;
        this.country = country;
        this.position = position;
        this.countryPosition = country + "::" + position;
        this.sortPartition = 1;
    }
}
