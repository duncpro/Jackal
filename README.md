# Jackal
[![Build Status](https://travis-ci.com/duncpro/jackal.svg?branch=master)](https://travis-ci.com/duncpro/jackal)
[![codecov](https://codecov.io/gh/duncpro/jackal/branch/master/graph/badge.svg?token=B5MZD14GUT)](https://codecov.io/gh/duncpro/jackal)
[![](https://jitpack.io/v/com.duncpro/jackal.svg)](https://jitpack.io/#com.duncpro/jackal)

Abstraction over RDS Aurora Data API and JDBC for Java 8.
Run your RDS Aurora Data API dependent applications locally using JDBC. 

## Overview
### Using Jackal with the Aurora Data API
In production and staging environments your application will likely be using
a real Aurora database. `AuroraServerlessRelationalDatabase` is an implementation of `RelationalDatabase` which wraps
the Aurora Data API Client included in AWS SDK v2.
```java
final RelationalDatabase db = new AuroraServerlessRelationalDatabase(/* */);
```
### Using Jackal with JDBC
In development/testing scenarios you're likely using a locally hosted or in-memory database.
Jackal provides a second implementation of `RelationalDatabase`
which wraps a standard JDBC `DataSource`. 
```java
final RelationalDatabase db = new DataSourceWrapper(/* */);
```
### Async Updates using CompletableFuture
Since the Aurora Data API is built on top of AWS SDK v2 and therefore Netty, performance can be improved by executing 
updates asynchronously. `SQLStatementBuilder#executeUpdateAsync` returns a `CompletableFuture` making it easy to perform multiple updates
simultaneously.
```java
final CompletableFuture<Void> u1 = db.prepareStatement("INSERT INTO person VALUES (?, ?, ?);")
        .withArguments("Duncan Proctor", 23, true)
        .executeUpdateAsync();

final CompletableFuture<Void> u2 = /* .. */
        
CompletableFuture.allOf(u1, u2);
```
### Sequential Updates
`SQLStatementBuilder#executeUpdate` is an alternative to the aforementioned method which blocks
the current thread until the update has completed. 

### Queries Using Java 8's Stream
`SQLStatementBuilder#executeQuery` returns a `Stream` which makes processing result sets much more ergonomic than
traditional JDBC. This function prefetches all results and closes any resources associated with the query.
```java
final Set<String> firstNames = db.prepareStatement("SELECT first_name FROM person LIMIT 10;")
        .executeQuery()
        .map(row -> row.get("first_name", String.class))
        .map(Optional::orElseThrow) // first_name is a NOT NULL column
        .collect(Collectors.toSet())
```
There is a second variant to this function, `executeQueryAsync` which returns a `CompletableFuture`. This is useful for 
performing queries concurrently.
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
    transaction.prepareStatement("INSERT INTO Person VALUES (?);")
        .withArguments("Bob")
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
The [Official RDS Data API Client Library](https://github.com/awslabs/rds-data-api-client-library-java) is
  lacking in a few key categories.
  - It has a dependency on AWS SDK v1 and can not support
    CompletableFuture. 
  - The parameterization syntax is too verbose, whereas the original JDBC
    syntax is much more concise.
  - It provides no facilities for local testing/development.
