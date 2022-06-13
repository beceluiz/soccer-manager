# Soccer Manager API

This project is a RESTful API for a Soccer Manager game. Requirements

* Users must be able to create an account and log in using the API.
* Each user can have only one team (user is identified by an email){
* When the user is signed up, they should get a team of 20 players (the system should generate players):
  * 3 goalkeepers
  * 6 defenders
  * 6 midfielders
  * 5 attackers
* Each player has an initial value of $1.000.000.
* Each team has an additional $5.000.000 to buy other players.
* When logged in, a user can see their team and player information
* The team has the following information:
  * Team name and team country (can be edited).
  * Team value (sum of player values).
* The Player has the following information
  * First name, last name, country (can be edited by a team owner).
  * Age (random number from 18 to 40) and market value.
* A team owner can set the player on a transfer list
* When a user places a player on a transfer list, they must set the asking price/value for this player. This value should be listed on a market list. When another user/team buys this player, they must be bought for this price.
* Each user should be able to see all players on a transfer list.
* With each transfer, team budgets are updated.
* When a player is transferred to another team, their value should be increased between 10 and 100 percent. Implement a random factor for this purpose.

# Endpoints

The API is composed of these endpoints:
* POST /user
  * creates an user and their respective team
* POST /auth
  * logs the user in
* GET /team
  * retrieves the logged user's team
* PUT /team
  * updates the logged user's team
* PUT /player/{player-id}
  * updates a player
* POST /offer
  * creates an offer / transfer a player to the market list
* GET /offer
  * searches the market list
* POST /purchase
  * purchases a player

## POST /user

* Creates an user account
* Creates the user's team
  * Assign a random team name
  * Assign a random country
  * Assign a $5,000,000.00 budget
  * Creates 20 players
    * Assign a random name to each player
    * Assign the same country as the team's
    * Assign a random age between 18 and 40 (inclusive)
    * Assign a $1,000,000.00 market value
    * Assign a random position between Goalkeeper, Midfielder, Defender, and Attacker

### Request
Body:
```json
{
	email: '<>',
	password: '<>'
}
```
### Response
Status code:
* 200 - User successfully created
* 400 - Multiple causes:
  * Invalid request body
  * E-mail address is not valid
  * E-mail address should be shorter than 128 characters
  * Password should be shorter than 256 characters
  * Password should be longer than 6 characters
  * User already exists
* 500 - Internal server error

## POST /auth

Logs the user in.

### Request
Body:
```json
{
	email: '<>',
	password: '<>'
}
```
### Response
Headers:
* Authorization - containing a Bearer token

Status code:
* 200 - Successfully logged in
* 400 - Invalid request body
* 404 - Invalid credentials
* 500 - Internal server error

## GET /team

Retrieves the logged user's team.

### Request
Headers:
* Authorization - containing a Bearer token

### Response
Body:
```json
{
  "id": "<team id / logged user email>",
  "name": "<team name>",
  "country": "<team country>",
  "value": "<string value format: 20000000.00>",
  "budget": "<string budget format: 20000000.00>",
  "players": [
    {
      "id": "<player uuid>",
      "firstName": "<player first name>",
      "lastName": "<player last name>",
      "country": "<player country>",
      "age": <numeric player age>,
      "value": "<string value format: 20000000.00>",
      "position": "DEFENDER|MIDFIELDER|ATTACKER|GOALKEEPER"
    },
    ...
  ]
}
```

Status code:
* 200 - OK
* 400 - Invalid Authorization token
* 500 - Internal server error

## PUT /team

Updates the logged user team's name and country.

### Request
Headers:
* Authorization - containing a Bearer token

Body:
```json
{
  "name": "<team's new name>",
  "country": "<team's new country>"
}
```

### Response

Status code:
* 200 - Successfully updated the team
* 400 
  * Invalid request body
  * Invalid Authorization token
* 500 - Internal server error

## PUT /player/{player-id}

Updates a player's first name, last name, and country.

### Request
Headers:
* Authorization - containing a Bearer token

Path parameters:
* playerId - Player's UUID

Body:
```json
{
  "firstName": "<player's new first name>",
  "lastName": "<player's new last name>",
  "country": "<player's new country>"
}
```

### Response

Status code:
* 200 - Successfully updated the player
* 400
  * Invalid request body
  * Invalid Authorization token
* 403 - Player doesn't belong to logged user's team
* 404 - Player not found
* 500 - Internal server error

## POST /offer

Creates an offer (transfers a player to market list)

### Request
Headers:
* Authorization - containing a Bearer token

Body:
```json
{
  "playerId": "<player's ID>",
  "price": "<price for that player>"
}
```

### Response

Status code:
* 200 - Successfully created the offer
* 400 - Multiple causes
  * Invalid Authorization token
  * Invalid request body
  * There's already an offer created for that player
* 403 - Player doesn't belong to logged user's team
* 404 - Player not found
* 500 - Internal server error

## GET /offer

Searches offers globally.

### Request
Headers:
* Authorization - containing a Bearer token

Query parameters:
* country - filters the offers by a single country
* position - filter the offer by a player position (MIDFIELDER / GOALKEEPER / ATTACKER / DEFENDER)
* pageSize - limits the amount of retrieved offers (default: 10)
* orderBy - field to sort the offers (price / discount)
* orderDirection - sort direction (ASC / DESC) (default: ASC if orderBy=price, DESC if orderBy=discount)
* exclusiveStartKey - the key of the last search's last element

### Response

Body:
```json
{
    "lastEvaluatedKey": {
        /* key to be used on next search's exclusiveStarKey,
         * if user wants to query the next page */
    },
    "offers": [
        {
            "id": "<offer id / player uuid>",
            "price": "<offer price, eg.: 900000.00>",
            "discount": "<offer discount (100% minus price divided by market value), eg.: 10.00>",
            "player": {
                "id": "<player uuid>",
                "firstName": "<player first name>",
                "lastName": "<player last name>",
                "country": "<player country>",
                "age": <player age>,
                "value": "<player market value, eg.: 1000000.00>",
                "position": "<ATTACKER|DEFENDER|GOALKEEPER|MIDFIELDER>"
            }
        },
```

Status code:
* 200 - Successfully searched the offers
* 400 - Invalid Authorization token
* 500 - Internal server error

## POST /purchase

Buys a player/offer.
* Decreases logged user's team budget
* Increases logged user's team value
* Increases player's market value
* Increases original player's team budget
* Decreases original player's team value
* Deletes offer
* Removes player from original team
* Adds player to new team

### Request
Headers:
* Authorization - containing a Bearer token

Body:
```json
{
  "playerId": "<player's ID>"
}
```

### Response

Status code:
* 200 - Successfully processed the purchase
* 400 - Multiple causes
  * Invalid Authorization token
  * Invalid request body
  * There's no offer for that player
  * Player is already in logged user's team
  * Not enough budget to purchase
* 404 - Player not found
* 500 - Internal server error

# Architecture

The API was built on AWS using AWS Cloud Developkment Kit (CDK). All the packages were built using Java 11 language and Maven build tool.

The architecture is composed by:
* 1 Cognito user pool
  * with 1 Cognito client
* 3 DynamoDB tables
  * Player
  * Team
  * Offer
    * with 8 Global Secondary Indexes (GSIs)
* 8 Lambda functions (one for each endpoint)
  * CreateUser
  * Login
  * GetTeam
  * UpdateTeam
  * UpdatePlayer
  * CreateOffer
  * SearchOffers
  * PurchasePlayer
* 1 API Gateway
  * with 8 routes (one for each lambda/endpoint)

The CDK implementation can be found in the [soccermanager-cdk package](./soccermanager-cdk) and the lambdas implementations can be found in the [soccermanager-lambdas directory](./soccermanager-lambdas).

For details on how to build/deploy, check [soccermanager-cdk README](./soccermanager-cdk/README.md).