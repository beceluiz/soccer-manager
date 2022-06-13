package com.luisguadagnin.soccermanager;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisguadagnin.soccermanager.dto.KeyResource;
import com.luisguadagnin.soccermanager.dto.OfferResponse;
import com.luisguadagnin.soccermanager.dto.OffersQueryResponse;
import com.luisguadagnin.soccermanager.dto.PlayerResponse;
import com.luisguadagnin.soccermanager.dto.SearchOffersRequest;
import com.luisguadagnin.soccermanager.dto.SearchOffersResponse;
import com.luisguadagnin.soccermanager.exception.BadRequestException;
import com.luisguadagnin.soccermanager.model.Offer;
import com.luisguadagnin.soccermanager.model.Player;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;
import com.luisguadagnin.soccermanager.repository.OfferRepository;
import com.luisguadagnin.soccermanager.repository.PlayerRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SearchOffersHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final ObjectMapper objectMapper;
    private final PlayerRepository playerRepository;
    private final OfferRepository offerRepository;

    public SearchOffersHandler() {
        objectMapper = new ObjectMapper();
        playerRepository = new PlayerRepository();
        offerRepository = new OfferRepository();
    }

    SearchOffersHandler(PlayerRepository playerRepository, OfferRepository offerRepository) {
        this.playerRepository = playerRepository;
        this.offerRepository = offerRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        String response;
        try {
            response = searchOffers(apiGatewayProxyRequestEvent);
        } catch (BadRequestException ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse(ex.getMessage()))
                    .withStatusCode(400);
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(buildErrorResponse("Internal server error"))
                    .withStatusCode(500);
        }
        return new APIGatewayProxyResponseEvent()
                .withBody(response)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withStatusCode(200);
    }

    private String searchOffers(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent) throws JsonProcessingException {
        Map<String, String> queryParameters = Optional.ofNullable(apiGatewayProxyRequestEvent.getQueryStringParameters()).orElse(new HashMap<>());
        SearchOffersRequest request = buildRequestFromQueryParameters(queryParameters);

        OffersQueryResponse offersQueryResponse = offerRepository.findByQuery(request);

        List<String> offerIds = offersQueryResponse.getOffers().stream().map(Offer::getId).collect(Collectors.toList());
        Map<String, Player> mapIdPlayer = playerRepository.findById(offerIds).stream().collect(Collectors.toMap(
                Player::getId,
                Function.identity()
        ));

        SearchOffersResponse searchOffersResponse = buildResponse(offersQueryResponse, mapIdPlayer);
        return objectMapper.writeValueAsString(searchOffersResponse);
    }

    private SearchOffersRequest buildRequestFromQueryParameters(Map<String, String> queryParameters) {
        String orderBy = queryParameters.getOrDefault("orderBy", "price");
        return SearchOffersRequest.builder()
                .country(queryParameters.get("country"))
                .position(Optional.ofNullable(queryParameters.get("position")).map(PlayerPosition::valueOf).orElse(null))
                .orderBy(orderBy)
                .orderDirection(queryParameters.getOrDefault("orderDirection", "price".equals(orderBy) ? "ASC" : "DESC"))
                .pageSize(Integer.parseInt(queryParameters.getOrDefault("pageSize", "10")))
                .exclusiveStartKey(convertExclusiveStartKey(queryParameters.get("exclusiveStartKey")))
                .build();
    }

    private Map<String, AttributeValue> convertExclusiveStartKey(String str) {
        if (str == null) return null;
        try {
            return objectMapper.readValue(str, KeyResource.class).toMap();
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid exclusiveStartKey");
        }
    }

    private SearchOffersResponse buildResponse(OffersQueryResponse offersQueryResponse, Map<String, Player> mapIdPlayer) {
        return SearchOffersResponse.builder()
                .lastEvaluatedKey(Optional.ofNullable(offersQueryResponse.getLastEvaluatedKey()).map(KeyResource::new).orElse(null))
                .offers(offersQueryResponse.getOffers().stream()
                        .map(offer -> buildOfferResponse(offer, mapIdPlayer))
                        .collect(Collectors.toList()))
                .build();
    }

    private OfferResponse buildOfferResponse(Offer offer, Map<String, Player> mapIdPlayer) {
        return OfferResponse.builder()
                .id(offer.getId())
                .discount(new BigDecimal(offer.getDiscount()).divide(new BigDecimal(100), 2, RoundingMode.UNNECESSARY).toString())
                .price(new BigDecimal(offer.getPrice()).divide(new BigDecimal(100), 2, RoundingMode.UNNECESSARY).toString())
                .player(buildPlayerResponse(mapIdPlayer.get(offer.getId())))
                .build();
    }

    private PlayerResponse buildPlayerResponse(Player player) {
        return PlayerResponse.builder()
                .value(player.getValue())
                .position(player.getPosition())
                .country(player.getCountry())
                .lastName(player.getLastName())
                .firstName(player.getFirstName())
                .age(player.getAge())
                .id(player.getId())
                .build();
    }

    private String buildErrorResponse(String message) {
        return "{ \"message\": \"" + message + "\" }";
    }

}
