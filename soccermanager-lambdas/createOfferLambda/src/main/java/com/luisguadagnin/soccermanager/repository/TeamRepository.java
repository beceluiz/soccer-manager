package com.luisguadagnin.soccermanager.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.luisguadagnin.soccermanager.configuration.AWSConfiguration;
import com.luisguadagnin.soccermanager.model.Team;

public class TeamRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public TeamRepository() {
        dynamoDBMapper = AWSConfiguration.getDynamoDBMapper();
    }

    public Team findById(String id) {
        return dynamoDBMapper.load(Team.class, id);
    }

}
