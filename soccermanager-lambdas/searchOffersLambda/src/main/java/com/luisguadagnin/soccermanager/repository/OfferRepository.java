package com.luisguadagnin.soccermanager.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.luisguadagnin.soccermanager.configuration.AWSConfiguration;
import com.luisguadagnin.soccermanager.dto.OffersQueryResponse;
import com.luisguadagnin.soccermanager.dto.SearchOffersRequest;
import com.luisguadagnin.soccermanager.model.Offer;
import com.luisguadagnin.soccermanager.model.enums.PlayerPosition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OfferRepository {

    private final DynamoDBMapper dynamoDBMapper;

    public OfferRepository() {
        dynamoDBMapper = AWSConfiguration.getDynamoDBMapper();
    }

    public OffersQueryResponse findByQuery(SearchOffersRequest request) {
        String indexName = buildIndexName(request);
        DynamoDBQueryExpression<Offer> queryExpression = new DynamoDBQueryExpression<Offer>()
                .withIndexName(indexName)
                .withKeyConditionExpression("#attr = :val")
                .withExpressionAttributeNames(buildExpressionAttributeNames(request))
                .withExpressionAttributeValues(buildExpressionAttributeValues(request))
                .withScanIndexForward("ASC".equals(request.getOrderDirection()))
                .withLimit(request.getPageSize())
                .withExclusiveStartKey(request.getExclusiveStartKey())
                .withConsistentRead(false);

        QueryResultPage<Offer> result = dynamoDBMapper.queryPage(Offer.class, queryExpression);
        List<Offer> queriedOffers = result.getResults();

        List<Offer> fetchedOffers = queriedOffers.stream()
                .map(dynamoDBMapper::load)
                .collect(Collectors.toList());
        return OffersQueryResponse.builder()
                .offers(fetchedOffers)
                .lastEvaluatedKey(result.getLastEvaluatedKey())
                .build();
    }

    private String buildIndexName(SearchOffersRequest request) {
        final String country = request.getCountry();
        final PlayerPosition position = request.getPosition();
        final String orderBy = request.getOrderBy();

        String indexSortKey = "price".equals(orderBy) ? "Price" : "Discount";
        String indexPartitionKey;
        if (country == null && position == null) indexPartitionKey = "Sort";
        else if (country == null) indexPartitionKey = "Position";
        else if (position == null) indexPartitionKey = "Country";
        else indexPartitionKey = "CountryPosition";
        return String.format("%s-%s-index", indexPartitionKey, indexSortKey);
    }

    private Map<String, AttributeValue> buildExpressionAttributeValues(SearchOffersRequest request) {
        String country = request.getCountry();
        PlayerPosition position = request.getPosition();

        AttributeValue attributeValue = new AttributeValue();
        if (country == null && position == null) {
            attributeValue = attributeValue.withN("1");
        } else if (country == null) {
            attributeValue = attributeValue.withS(request.getPosition().toString());
        } else if (position == null) {
            attributeValue = attributeValue.withS(request.getCountry());
        } else {
            attributeValue = attributeValue.withS(request.getCountry() + "::" + request.getPosition());
        }
        return Map.of(":val", attributeValue);
    }

    private Map<String, String> buildExpressionAttributeNames(SearchOffersRequest request) {
        String country = request.getCountry();
        PlayerPosition position = request.getPosition();

        String attributeName;
        if (country == null && position == null) {
            attributeName = "sort_partition";
        } else if (country == null) {
            attributeName = "position";
        } else if (position == null) {
            attributeName = "country";
        } else {
            attributeName = "country_position";
        }
        return Map.of("#attr", attributeName);
    }

}
