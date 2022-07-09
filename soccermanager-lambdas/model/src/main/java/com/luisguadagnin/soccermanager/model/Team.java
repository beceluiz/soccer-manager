package com.luisguadagnin.soccermanager.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@DynamoDBTable(tableName = "Team")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "name")
    private String name;

    @DynamoDBAttribute(attributeName = "country")
    private String country;

    @DynamoDBAttribute(attributeName = "value")
    private String value;

    @DynamoDBAttribute(attributeName = "budget")
    private String budget;

    @DynamoDBAttribute(attributeName = "playersId")
    private Set<String> playersId;

}
