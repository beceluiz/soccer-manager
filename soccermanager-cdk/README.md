# Intro

This is a CDK Development project with Java.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

# Architecture

The architecture is composed by:
* 1 Cognito user pool
    * with 1 Cognito client
* 3 DynamoDB tables
    * Player
    * Team
    * Offer
        * with 8 Global Secondary Indexes (GSIs)
* 8 Lambda functions
    * CreateUser
    * Login
    * GetTeam
    * UpdateTeam
    * UpdatePlayer
    * CreateOffer
    * SearchOffers
    * PurchasePlayer
* 1 API Gateway
    * with 8 routes (one for each lambda)

# Building the lambdas

The lambdas are in the [soccermanager-lambdas directory](../soccermanager-lambdas). You can go inside each lambda's directory and build it using the following command:
```shell
mvn clean install
```

If this is your first time, you have to build all of them before deploying.

```shell
cd ../soccermanager-lambdas/ && \
cd ./model/ && mvn clean install && cd .. && \
cd ./createOfferLambda/ && mvn clean install && cd .. && \
cd ./createUserLambda/ && mvn clean install && cd .. && \
cd ./getTeamLambda/ && mvn clean install && cd .. && \
cd ./loginLambda/ && mvn clean install && cd .. && \
cd ./purchasePlayerLambda/ && mvn clean install && cd .. && \
cd ./searchOffersLambda/ && mvn clean install && cd .. && \
cd ./updatePlayerLambda/ && mvn clean install && cd .. && \
cd ./updateTeamLambda/ && mvn clean install && cd ..
```

# Deploying the application

To deploy the application, first make sure:
* you have installed AWS CLI v2
* you have installed Node
* you have installed CDK via NPM
* you have configured your AWS credentials using `aws configure`
* you have built all the lambdas' packages (as stated in previous section)

You have to simply run these four commands inside the soccermanager-cdk package:
```shell
mvn package
cdk synth
cdk bootstrap
cdk deploy
```

It takes less than 5 minutes and you're all set!