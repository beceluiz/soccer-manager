package com.luisguadagnin.soccermanager.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.luisguadagnin.soccermanager.configuration.AWSConfiguration;
import com.luisguadagnin.soccermanager.model.Player;

public class PlayerRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public PlayerRepository() {
        dynamoDBMapper = AWSConfiguration.getDynamoDBMapper();
    }

    public Player findById(String playerId) {
        return dynamoDBMapper.load(Player.class, playerId);
    }

    public void save(Player player) {
        dynamoDBMapper.save(player);
    }

}
