package com.luisguadagnin.soccermanager;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisguadagnin.soccermanager.dto.CreateOfferRequest;
import com.luisguadagnin.soccermanager.exception.BadRequestException;
import com.luisguadagnin.soccermanager.exception.ForbiddenException;
import com.luisguadagnin.soccermanager.exception.NotFoundException;
import com.luisguadagnin.soccermanager.model.Offer;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.Team;
import com.luisguadagnin.soccermanager.repository.OfferRepository;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;
import com.luisguadagnin.soccermanager.repository.TeamRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class CreateOfferHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final OfferRepository offerRepository;

    public CreateOfferHandler() {
        objectMapper = new ObjectMapper();
        teamRepository = new TeamRepository();
        playerRepository = new PlayerRepository();
        offerRepository = new OfferRepository();
    }

    CreateOfferHandler(TeamRepository teamRepository, PlayerRepository playerRepository, OfferRepository offerRepository) {
        this.objectMapper = new ObjectMapper();
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.offerRepository = offerRepository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        try {
            createOffer(apiGatewayProxyRequestEvent);
        } catch (BadRequestException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(400);
        } catch (ArithmeticException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("\"price\" field is invalid"))
                    .withStatusCode(400);
        } catch(ForbiddenException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(403);
        } catch(NotFoundException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(404);
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Internal server error"))
                    .withStatusCode(500);
        }

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200);
    }

    private void createOffer(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        String username = extractUsername(apiGatewayProxyRequestEvent);
        CreateOfferRequest createOfferRequest = convertInput(apiGatewayProxyRequestEvent.getBody());

        validateCreateOfferRequest(createOfferRequest);
        String playerId = createOfferRequest.getPlayerId();

        BigDecimal decimalPrice = new BigDecimal(createOfferRequest.getPrice());
        long priceLong = decimalPrice.multiply(new BigDecimal("100")).longValueExact(); // removes fractional part

        Player player = playerRepository.findById(playerId);
        if (player == null) throw new NotFoundException("Player doesn't exist");

        Team team = teamRepository.findById(username);
        if (!team.getPlayersId().contains(playerId)) throw new ForbiddenException("Player doesn't belong to logged user's team");

        Offer existingOffer = offerRepository.findById(playerId);
        validate(existingOffer == null, "An offer for this player already exists");

        BigDecimal decimalValue = new BigDecimal(player.getValue());
        BigDecimal decimalDiscount = BigDecimal.ONE.subtract(decimalPrice.divide(decimalValue, 4, RoundingMode.HALF_DOWN));
        int discountInt = decimalDiscount.multiply(new BigDecimal("10000")).intValue(); // turns into % and removes fractional part

        Offer newOffer = Offer.builder()
                .id(playerId)
                .country(player.getCountry())
                .discount(discountInt)
                .price(priceLong)
                .position(player.getPosition())
                .build();

        offerRepository.save(newOffer);
    }

    @SuppressWarnings("unchecked")
    private String extractUsername(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) {
        try {
            Map<String, Object> authorizer = apiGatewayProxyRequestEvent.getRequestContext().getAuthorizer();
            Map<String, Object> jwt = (Map<String, Object>) authorizer.get("jwt");
            Map<String, Object> claims = (Map<String, Object>) jwt.get("claims");
            return (String) claims.get("username");
        } catch (ClassCastException ex) {
            throw new RuntimeException("Unable to process authorization token", ex);
        }
    }

    private CreateOfferRequest convertInput(String body) {
        try {
            return objectMapper.readValue(body, CreateOfferRequest.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid request body");
        }
    }

    private void validateCreateOfferRequest(CreateOfferRequest createOfferRequest) {
        validate(!StringUtils.isNullOrEmpty(createOfferRequest.getPlayerId()), "\"playerId\" field is empty");
        validate(!StringUtils.isNullOrEmpty(createOfferRequest.getPrice()), "\"price\" field is empty");
    }

    private void validate(boolean isValid, String failMessage) {
        if(!isValid) {
            throw new BadRequestException(failMessage);
        }
    }

    private String buildErrorResponse(String message) {
        return "{ \"message\": \"" + message + "\" }";
    }

}
