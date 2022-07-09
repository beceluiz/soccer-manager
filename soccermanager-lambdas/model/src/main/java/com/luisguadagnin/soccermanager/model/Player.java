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

@DynamoDBTable(tableName = "Player")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "firstName")
    private String firstName;

    @DynamoDBAttribute(attributeName = "lastName")
    private String lastName;

    @DynamoDBAttribute(attributeName = "country")
    private String country;

    @DynamoDBAttribute(attributeName = "age")
    private int age;

    @DynamoDBAttribute(attributeName = "value")
    private String value;

    @DynamoDBAttribute(attributeName = "position")
    @DynamoDBTypeConvertedEnum
    private PlayerPosition position;

    @DynamoDBAttribute(attributeName = "teamId")
    private String teamId;

}
