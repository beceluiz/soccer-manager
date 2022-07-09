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

    public List<Player> findById(Collection<String> playersId) {
        List<Player> emptyPlayers = playersId.stream().map(playerId -> {
            Player player = new Player();
            player.setId(playerId);
            return player;
        }).collect(Collectors.toList());
        Map<String, List<Object>> result = dynamoDBMapper.batchLoad(emptyPlayers);
        return result.get("Player").stream()
                .map(obj -> (Player) obj)
                .collect(Collectors.toList());
    }

}
