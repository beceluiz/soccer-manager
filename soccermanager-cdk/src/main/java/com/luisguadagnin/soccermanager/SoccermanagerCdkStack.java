package com.luisguadagnin.soccermanager;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.authorizers.alpha.HttpUserPoolAuthorizer;
import software.amazon.awscdk.services.apigatewayv2.authorizers.alpha.HttpUserPoolAuthorizerProps;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.HttpLambdaIntegrationProps;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.PasswordPolicy;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolClientProps;
import software.amazon.awscdk.services.cognito.UserPoolProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.GlobalSecondaryIndexProps;
import software.amazon.awscdk.services.dynamodb.ProjectionType;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.FunctionProps;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SoccermanagerCdkStack extends Stack {

    public SoccermanagerCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Objects.requireNonNull(props.getEnv());
        String region = props.getEnv().getRegion();
        String account = props.getEnv().getAccount();

        UserPool cognitoUserPool = new UserPool(this, "SoccerManagerUsers", UserPoolProps.builder()
                .userPoolName("SoccerManagerUsers")
                .passwordPolicy(PasswordPolicy.builder()
                        .minLength(6)
                        .requireDigits(false) // requires numbers
                        .requireLowercase(false)
                        .requireUppercase(false)
                        .requireSymbols(false) // requires special characters
                        .build())
                .build());

        String userPoolId = cognitoUserPool.getUserPoolId();

        UserPoolClient userPoolClient = new UserPoolClient(this, "SoccerManagerClient", UserPoolClientProps.builder()
                .userPool(cognitoUserPool)
                .authFlows(AuthFlow.builder()
                        .adminUserPassword(true)
                        .userPassword(false)
                        .userSrp(false)
                        .custom(false)
                        .build())
                .build());

        String userPoolClientId = userPoolClient.getUserPoolClientId();

        PolicyStatement cognitoAllowCreateUserPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "cognito-idp:AdminGetUser",
                        "cognito-idp:AdminCreateUser",
                        "cognito-idp:AdminSetUserPassword"
                ))
                .resources(List.of(
                        "arn:aws:cognito-idp:" + region + ":" + account + ":userpool/" + userPoolId
                ))
                .build();

        PolicyStatement cognitoAllowAuthPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "cognito-idp:AdminInitiateAuth"
                ))
                .resources(List.of(
                        "arn:aws:cognito-idp:" + region + ":" + account + ":userpool/" + userPoolId
                ))
                .build();

        Table playerTable = new Table(this, "PlayerTable", TableProps.builder()
                .tableName("Player")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        Table teamTable = new Table(this, "TeamTable", TableProps.builder()
                .tableName("Team")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        Table offerTable = new Table(this, "OfferTable", TableProps.builder()
                .tableName("Offer")
                .partitionKey(Attribute.builder()
                        .name("id")
                        .type(AttributeType.STRING)
                        .build())
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("Sort-Discount-index")
                .partitionKey(Attribute.builder()
                        .name("sort_partition")
                        .type(AttributeType.NUMBER)
                        .build())
                .sortKey(Attribute.builder()
                        .name("discount")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("Sort-Price-index")
                .partitionKey(Attribute.builder()
                        .name("sort_partition")
                        .type(AttributeType.NUMBER)
                        .build())
                .sortKey(Attribute.builder()
                        .name("price")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("Country-Discount-index")
                .partitionKey(Attribute.builder()
                        .name("country")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("discount")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("Country-Price-index")
                .partitionKey(Attribute.builder()
                        .name("country")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("price")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("Position-Discount-index")
                .partitionKey(Attribute.builder()
                        .name("position")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("discount")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("Position-Price-index")
                .partitionKey(Attribute.builder()
                        .name("position")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("price")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("CountryPosition-Discount-index")
                .partitionKey(Attribute.builder()
                        .name("country_position")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("discount")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        offerTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("CountryPosition-Price-index")
                .partitionKey(Attribute.builder()
                        .name("country_position")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("price")
                        .type(AttributeType.NUMBER)
                        .build())
                .projectionType(ProjectionType.KEYS_ONLY)
                .build());

        Function createUserFunction = new Function(this, "CreateUserFunction", FunctionProps.builder()
                .functionName("CreateUserFunction")
                .environment(Map.of("COGNITO_USER_POOL_ID", userPoolId))
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/createUserLambda/target/createuserlambda.jar"))
                .handler("com.luisguadagnin.soccermanager.CreateUserHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        createUserFunction.addToRolePolicy(cognitoAllowCreateUserPolicy);
        playerTable.grantWriteData(createUserFunction);
        teamTable.grantWriteData(createUserFunction);

        Function loginFunction = new Function(this, "LoginFunction", FunctionProps.builder()
                .functionName("LoginFunction")
                .environment(Map.of(
                        "COGNITO_USER_POOL_ID", userPoolId,
                        "COGNITO_USER_POOL_CLIENT_ID", userPoolClientId))
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/loginLambda/target/loginlambda.jar"))
                .handler("com.luisguadagnin.soccermanager.LoginHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        loginFunction.addToRolePolicy(cognitoAllowAuthPolicy);

        Function getTeamFunction = new Function(this, "GetTeamFunction", FunctionProps.builder()
                .functionName("GetTeamFunction")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/getTeamLambda/target/getteamlambda.jar"))
                .handler("com.luisguadagnin.soccermanager.GetTeamHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        playerTable.grantReadData(getTeamFunction);
        teamTable.grantReadData(getTeamFunction);

        Function updateTeamFunction = new Function(this, "UpdateTeamFunction", FunctionProps.builder()
                .functionName("UpdateTeamFunction")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/updateTeamLambda/target/updateteamlambda.jar"))
                .handler("com.luisguadagnin.soccermanager.UpdateTeamHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        teamTable.grantReadWriteData(updateTeamFunction);

        Function updatePlayerFunction = new Function(this, "UpdatePlayerFunction", FunctionProps.builder()
                .functionName("UpdatePlayerFunction")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/updatePlayerLambda/target/updateplayerlambda.jar"))
                .handler("com.luisguadagnin.soccermanager.UpdatePlayerHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        teamTable.grantReadData(updatePlayerFunction);
        playerTable.grantReadWriteData(updatePlayerFunction);

        Function createOfferFunction = new Function(this, "CreateOfferFunction", FunctionProps.builder()
                .functionName("CreateOfferFunction")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/createOfferLambda/target/createofferlambda.jar"))
                .handler("com.luisguadagnin.soccermanager.CreateOfferHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        teamTable.grantReadData(createOfferFunction);
        playerTable.grantReadData(createOfferFunction);
        offerTable.grantReadWriteData(createOfferFunction);

        Function searchOffersFunction = new Function(this, "SearchOffersFunction", FunctionProps.builder()
                .functionName("SearchOffersFunction")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/searchOffersLambda/target/searchofferslambda.jar"))
                .handler("com.luisguadagnin.soccermanager.SearchOffersHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        playerTable.grantReadData(searchOffersFunction);
        offerTable.grantReadData(searchOffersFunction);

        Function purchasePlayerFunction = new Function(this, "PurchasePlayerFunction", FunctionProps.builder()
                .functionName("PurchasePlayerFunction")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset("../soccermanager-lambdas/purchasePlayerLambda/target/purchaseplayerlambda.jar"))
                .handler("com.luisguadagnin.soccermanager.PurchasePlayerHandler")
                .memorySize(512)
                .timeout(Duration.minutes(1))
                .build());

        playerTable.grantReadWriteData(purchasePlayerFunction);
        teamTable.grantReadWriteData(purchasePlayerFunction);
        offerTable.grantReadWriteData(purchasePlayerFunction);

        HttpApi httpApi = new HttpApi(this, "soccer-manager-api", HttpApiProps.builder()
                .apiName("soccer-manager-api")
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/user")
                .methods(List.of(HttpMethod.POST))
                .integration(new HttpLambdaIntegration("CreateUserIntegration", createUserFunction, HttpLambdaIntegrationProps.builder().build()))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/auth")
                .methods(List.of(HttpMethod.POST))
                .integration(new HttpLambdaIntegration("LoginIntegration", loginFunction, HttpLambdaIntegrationProps.builder().build()))
                .build());

        HttpUserPoolAuthorizer userPoolAuthorizer = new HttpUserPoolAuthorizer("SoccerManagerUserPoolAuthorizer", cognitoUserPool, HttpUserPoolAuthorizerProps.builder()
                .authorizerName("SoccerManagerUserPoolAuthorizer")
                .identitySource(List.of("$request.header.Authorization"))
                .userPoolClients(List.of(userPoolClient))
                .userPoolRegion(region)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/team")
                .methods(List.of(HttpMethod.GET))
                .integration(new HttpLambdaIntegration("GetTeamIntegration", getTeamFunction, HttpLambdaIntegrationProps.builder().build()))
                .authorizer(userPoolAuthorizer)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/team")
                .methods(List.of(HttpMethod.PUT))
                .integration(new HttpLambdaIntegration("UpdateTeamIntegration", updateTeamFunction, HttpLambdaIntegrationProps.builder().build()))
                .authorizer(userPoolAuthorizer)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/player/{playerId}")
                .methods(List.of(HttpMethod.PUT))
                .integration(new HttpLambdaIntegration("UpdatePlayerIntegration", updatePlayerFunction, HttpLambdaIntegrationProps.builder().build()))
                .authorizer(userPoolAuthorizer)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/offer")
                .methods(List.of(HttpMethod.POST))
                .integration(new HttpLambdaIntegration("CreateOfferIntegration", createOfferFunction, HttpLambdaIntegrationProps.builder().build()))
                .authorizer(userPoolAuthorizer)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/offer")
                .methods(List.of(HttpMethod.GET))
                .integration(new HttpLambdaIntegration("SearchOffersIntegration", searchOffersFunction, HttpLambdaIntegrationProps.builder().build()))
                .authorizer(userPoolAuthorizer)
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/purchase")
                .methods(List.of(HttpMethod.POST))
                .integration(new HttpLambdaIntegration("PurchasePlayerIntegration", purchasePlayerFunction, HttpLambdaIntegrationProps.builder().build()))
                .authorizer(userPoolAuthorizer)
                .build());

        new CfnOutput(this, "HttpApi", CfnOutputProps.builder()
                .description("Url for HTTP Api")
                .value(httpApi.getApiEndpoint())
                .build());
    }
}
