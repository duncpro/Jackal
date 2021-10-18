# Jackal
[![Build Status](https://travis-ci.com/duncpro/jackal.svg?branch=master)](https://travis-ci.com/duncpro/jackal)
[![codecov](https://codecov.io/gh/duncpro/jackal/branch/master/graph/badge.svg?token=B5MZD14GUT)](https://codecov.io/gh/duncpro/jackal)
[![](https://jitpack.io/v/com.duncpro/jackal.svg)](https://jitpack.io/#com.duncpro/jackal)

Wrapper around RDS Data API (AWS SDK v2) with support for local testing.
Jackal makes it possible to build applications that take advantage of the scalability provided by the Aurora Data API,
but can also be run locally during development and testing.

## Overview
### Using Jackal with the Aurora Data API
In production and staging environments your application will be using
a real Aurora database. `AmazonDataAPIDatabase` is an implementation of `RelationalDatabase` which wraps
the Aurora Data API Client included in AWS SDK v2.
```java
final RelationalDatabase db = new AmazonDataAPIDatabase(/* */);
```
### Using Jackal with JDBC
In development/testing scenarios it's advantageous to use a locally hosted or in-memory database instead
of an actual Aurora database. Among other advantages, using a JDBC database enables you to potentially work offline,
save on AWS costs, and develop in a more transparent environment. Additionally, the included JDBC support
provides an escape-hatch if you ever choose to move away from AWS/RDS.

Jackal provides a second implementation of `RelationalDatabase`
which wraps a standard JDBC `DataSource`. 
```java
final RelationalDatabase db = new DataSourceWrapper(/* */);
```
### Parallel Updates using CompletableFuture
Since the Aurora Data API is built on top of AWS SDK v2 and therefore HTTP, performance can be improved by running updates in parallel. 
`SQLStatementBuilder#startUpdate` returns a `CompletableFuture` making it easy to perform multiple updates
simultaneously.
```java
final CompletableFuture<Void> u1 = db.prepareStatement("INSERT INTO person VALUES (?, ?, ?);")
        .withArguments("Duncan Proctor", 23, true)
        .startUpdate();

final CompletableFuture<Void> u2 = /* .. */
        
CompletableFuture.allOf(u1, u2);
```
### Sequential Updates
In some cases, parallelization of updates introduces complexity without adding much of 
a performance benefit, like in a short-lived AWS Lambda function. For these instances, consider using
the `SQLStatementBuilder#executeUpdate` convenience method. This will block the current thread until
the update is complete.

### Queries Using Java 8's Stream
`SQLStatementBuilder#executeQuery` returns a `Stream` which makes processing result sets much more ergonomic than
traditional JDBC.
```java
final Set<String> firstNames = = db.prepareStatement("SELECT first_name FROM person LIMIT 10;")
        .executeQuery()
        .map(row -> row.get("first_name", String.class))
        .map(Optional::orElseThrow) // first_name is a NOT NULL column
        .collect(Collectors.toSet())
```
### Incrementally Fetching Query Results
The aforementioned function, `executeQuery`, prefetches all results and closes any resources associated with the query. 
In some cases prefetching might detriment performance so the function `startQuery` is provided as a companion.
This function incrementally fetches rows from the database, but only If the implementation of `RelationalDatabase`
which provided the `SQLStatementBuilder` supports incremental fetching.
```java
try (final var results = db.prepareStatement("SELECT first_name FROM person ORDER BY first_name DESC;")
        .startQuery()) {
    
    match = results.map(row -> row.get("first_name", String.class))
        .map(Optional::orElseThrow)
        .map(/* kick off some CompletableFutures */)
        .filter(/* Some expensive filtering function */)
        .findFirst();
}
```
Currently, only `DataSourceWrapper` supports incremental fetching. All `AmazonDataAPIDatabase` queries
are prefetched regardless of which query function is used.

If a database error occurs while fetching a row, then `UncheckedRelationalDatabaseException`
is thrown by the terminal operator function, for example: `collect`.
### JDBC-like Parameterization
Jackal supports statement parameterization using JDBC-like syntax.

```java
db.prepareStatement("INSERT INTO person VALUES (?, ?, ?);")
        .withArguments("Duncan Proctor", 23, true)
        .executeUpdate()
```

### Transaction API
Transactions must be explicitly committed using the `commit` method.
Rollbacks however are implicit. If `TransactionHandle#close` is called before
`TransactionHandle#commit`, like in the case of a mid-transaction exception, then
the transaction will be automatically rolled back.
```java
try (final var transaction = db.startTransaction()) {
    transaction.prepareStatement("ALTER TABLE Person ADD last_name VARCHAR;")
        .executeUpdate();
    transaction.commit();
} catch (RelationalDatabaseException e) {
    e.printStackTrace();
}
```
### Exceptions
`RelationalDatabaseException` serves as an abstraction over Java's `SQLException` and RDS's `SdkException`.
All Jackal functions throw `RelationalDatabaseException` but the underlying platform-specific exception
can still be accessed via `Exception#getCause` if necessary.

## Motivation
The [Official Data API Client Library](https://github.com/awslabs/rds-data-api-client-library-java) is
  lacking in a few key categories.
  - It has a dependency on AWS SDK v1 and can not support
    CompletableFuture. 
  - The parameterization syntax is too verbose, whereas the original JDBC
    syntax is much more concise.
  - It provides no facilities for local testing/development.
