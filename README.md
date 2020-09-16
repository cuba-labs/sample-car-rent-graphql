# CUBA Sample Project - Car Rent
Sample CUBA project with non trivial model, which used as base for tests in 
[@cuba-platform/front-generator](https://github.com/cuba-platform/frontend). 

This fork of Car Rent App provides examples of using GraphQL with CUBA App.

## GraphQL Endpoints
Main GraphQL endpoint<br>
[http://localhost:8080/app-portal/graphql]()

GraphiQL<br>
[http://localhost:8080/app-portal/graphiql]()

Generated Schema<br>
[http://localhost:8080/app-portal/graphql/schema]()

## Data
### Users
admin/admin<br>
mechanic/1
manager/2

## Development
### Create Init Script With Data Already Added in App
* Export data from table <TABLE_NAME>
```bash
sudo -u postgres pg_dump --table=<TABLE_NAME> --data-only --column-inserts scr
```
* Paste generated script to [30.create-db.sql](modules/core/db/init/hsql/30.create-db.sql) 
* Script will be executed when database will be created next time  