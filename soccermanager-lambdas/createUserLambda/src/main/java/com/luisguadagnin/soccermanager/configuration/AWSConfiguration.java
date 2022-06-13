package com.luisguadagnin.soccermanager.configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;

public class AWSConfiguration {

    private static String awsAccessKeyId;
    private static String awsSecretKey;
    private static String awsRegion;
    private static String awsSessionToken;
    private static AWSCredentials awsCredentials;
    private static AWSCredentialsProvider awsCredentialsProvider;
    private static String cognitoUserPoolId;
    private static AmazonDynamoDB amazonDynamoDB;
    private static DynamoDBMapper dynamoDBMapper;

    private AWSConfiguration() {}

    public static String getAwsAccessKeyId() {
        if (awsAccessKeyId == null) {
            awsAccessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
        }
        return awsAccessKeyId;
    }

    public static String getAwsSecretKey() {
        if (awsSecretKey == null) {
            awsSecretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        }
        return awsSecretKey;
    }

    public static String getAwsRegion() {
        if (awsRegion == null) {
            awsRegion = System.getenv("AWS_REGION");
        }
        return awsRegion;
    }

    public static String getAwsSessionToken() {
        if (awsSessionToken == null) {
            awsSessionToken = System.getenv("AWS_SESSION_TOKEN");
        }
        return awsSessionToken;
    }

    public static AWSCredentials getAwsCredentials() {
        if (awsCredentials == null) {
            awsCredentials = new BasicSessionCredentials(getAwsAccessKeyId(), getAwsSecretKey(), getAwsSessionToken());
        }
        return awsCredentials;
    }

    public static AWSCredentialsProvider getAwsCredentialsProvider() {
        if (awsCredentialsProvider == null) {
            awsCredentialsProvider = new AWSStaticCredentialsProvider(getAwsCredentials());
        }
        return awsCredentialsProvider;
    }

    public static String getCognitoUserPoolId() {
        if (cognitoUserPoolId == null) {
            cognitoUserPoolId = System.getenv("COGNITO_USER_POOL_ID");
        }
        return cognitoUserPoolId;
    }

    public static AmazonDynamoDB getAmazonDynamoDB() {
        if (amazonDynamoDB == null) {
            amazonDynamoDB = AmazonDynamoDBClient.builder()
                    .withCredentials(getAwsCredentialsProvider())
                    .withRegion(getAwsRegion())
                    .build();
        }
        return amazonDynamoDB;
    }

    public static DynamoDBMapper getDynamoDBMapper() {
        if (dynamoDBMapper == null) {
            dynamoDBMapper = new DynamoDBMapper(getAmazonDynamoDB());
        }
        return dynamoDBMapper;
    }

}
