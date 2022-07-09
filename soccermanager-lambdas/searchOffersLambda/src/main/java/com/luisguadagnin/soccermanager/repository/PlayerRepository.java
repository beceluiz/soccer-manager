package com.luisguadagnin.soccermanager.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.luisguadagnin.soccermanager.configuration.AWSConfiguration;
import com.luisguadagnin.soccermanager.model.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public PlayerRepository() {
        dynamoDBMapper = AWSConfiguration.getDynamoDBMapper();
    }

    public List<Player> findById(Collection<String> ids) {
        List<Player> dummyPlayers = ids.stream().map(id -> Player.builder().id(id).build()).collect(Collectors.toList());
        Map<String, List<Object>> result = dynamoDBMapper.batchLoad(dummyPlayers);
        List<Object> playersObj = result.get("Player");
        return playersObj.stream().map(playerObj -> (Player) playerObj).collect(Collectors.toList());
    }

}
