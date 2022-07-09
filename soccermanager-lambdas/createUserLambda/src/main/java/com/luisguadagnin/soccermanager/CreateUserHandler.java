package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.luisguadagnin.soccermanager.clients.CognitoClient;
import com.luisguadagnin.soccermanager.dto.CreateUserRequest;
import com.luisguadagnin.soccermanager.exception.BadRequestException;
import com.luisguadagnin.soccermanager.exception.UserAlreadyExistsException;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;
import com.luisguadagnin.soccermanager.util.EmailValidator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final CognitoClient cognitoClient;
    private final Faker faker;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public CreateUserHandler() {
        objectMapper = new ObjectMapper();
        cognitoClient = new CognitoClient();
        faker = new Faker();
        teamRepository = new TeamRepository();
        playerRepository = new PlayerRepository();
    }

    CreateUserHandler(CognitoClient cognitoClient, TeamRepository teamRepository, PlayerRepository playerRepository) {
        this.objectMapper = new ObjectMapper();
        this.faker = new Faker();

        this.cognitoClient = cognitoClient;
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {

            createUser(apiGatewayProxyRequestEvent);

        } catch (BadRequestException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(400);
        } catch (UserAlreadyExistsException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("User already exists"))
                    .withStatusCode(400);
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Internal server error"))
                    .withStatusCode(500);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200);
    }

    public void createUser(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        CreateUserRequest createUserRequest = convertInput(apiGatewayProxyRequestEvent.getBody());

        validateCreateUserRequest(createUserRequest);

        cognitoClient.createUser(createUserRequest.getEmail(), createUserRequest.getPassword());

        Team team = buildTeam(createUserRequest.getEmail());
        List<Player> players = buildPlayers(team);

        teamRepository.save(team);
        players.forEach(playerRepository::save);
    }

    private CreateUserRequest convertInput(String body) {
        try {
            CreateUserRequest request = objectMapper.readValue(body, CreateUserRequest.class);
            request.setPassword(request.getPassword().trim());
            return request;
        } catch(JsonProcessingException e) {
            throw new BadRequestException("Invalid request body");
        }
    }

    private void validateCreateUserRequest(CreateUserRequest createUserRequest) {
        validate(!StringUtils.isNullOrEmpty(createUserRequest.getEmail()), "\"email\" field is empty");
        validate(!StringUtils.isNullOrEmpty(createUserRequest.getPassword()), "\"password\" field is empty");
        validate(EmailValidator.isValid(createUserRequest.getEmail()), "E-mail address is not valid");
        validate(createUserRequest.getEmail().length() <= 128, "E-mail address should be shorter than 128 characters");
        validate(createUserRequest.getPassword().length() <= 256, "Password should be shorter than 256 characters");
        validate(createUserRequest.getPassword().length() >= 6, "Password should be longer than 6 characters");
    }

    private void validate(boolean isValid, String failMessage) {
        if(!isValid) {
            throw new BadRequestException(failMessage);
        }
    }

    private Team buildTeam(String userEmail) {
        return Team.builder()
                .id(userEmail)
                .country(faker.country().name())
                .name(faker.team().name())
                .budget("5000000.00")
                .value("20000000.00")
                .playersId(new HashSet<>())
                .build();
    }

    private List<Player> buildPlayers(Team team) {
        List<Player> players = new ArrayList<>();
        players.addAll(buildPlayers(team, PlayerPosition.GOALKEEPER, 3));
        players.addAll(buildPlayers(team, PlayerPosition.DEFENDER, 6));
        players.addAll(buildPlayers(team, PlayerPosition.MIDFIELDER, 6));
        players.addAll(buildPlayers(team, PlayerPosition.ATTACKER, 5));
        team.getPlayersId().addAll(players.stream().map(Player::getId).collect(Collectors.toList()));
        return players;
    }

    private List<Player> buildPlayers(Team team, PlayerPosition position, int amount) {
        List<Player> players = new ArrayList<>();
        for(int i = 0; i < amount; i++) {
            players.add(buildPlayer(team, position));
        }
        return players;
    }

    private Player buildPlayer(Team team, PlayerPosition position) {
        return Player.builder()
                .id(UUID.randomUUID().toString())
                .age(generateRandomNumberBetween(18, 40))
                .teamId(team.getId())
                .firstName(faker.name().firstName())
                .lastName(faker.name().lastName())
                .position(position)
                .country(team.getCountry())
                .value("1000000.00")
                .build();
    }

    private int generateRandomNumberBetween(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    private String buildErrorResponse(String message) {
        return "{ \"message\": \"" + message + "\" }";
    }

}
