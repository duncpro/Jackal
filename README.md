# Jackal
[![Build Status](https://travis-ci.com/duncpro/jackal.svg?branch=master)](https://travis-ci.com/duncpro/jackal)
[![codecov](https://codecov.io/gh/duncpro/jackal/branch/master/graph/badge.svg?token=B5MZD14GUT)](https://codecov.io/gh/duncpro/jackal)

Wrapper around RDS Data API (AWS SDK v2).

## Getting Started
```java
final AsyncDatabase db = new AmazonDataAPIDatabase(/* */);
```

## Features
### Fully embraces Java 8's CompletableFuture
All updates return `CompletableFuture` and all queries return `Stream`.
```java
final Set<String> firstNames;
try (final var results = db.prepareStatement("SELECT first_name FROM person")
        .executeQuery()) {
    
        firstNames = results.map(row -> row.get("first_name", String.class))
            .map(Optional::orElseThrow) // first_name is a NOT NULL column
            .collect(Collectors.toSet());
}

```
### JDBC-like Parameterization
```java
db.prepareStatement("INSERT INTO person VALUES (?, ?, ?);")
        .withArguments("Duncan Proctor", 23, true)
        .executeUpdate()
        .join();
```
### JDBC DataSource Implementation
In addition to `AmazonDataAPIDatabase`, this library includes a second implementation of `AsyncDatabase`, 
called `DataSourceAsyncWrapper`. This class wraps a standard JDBC `DataSource` with `AsyncDatabase`
making it easy to test database-related code locally.
```java
final AsyncDatabase db = new DataSourceAsyncWrapper(/* */);
```
### Transactions
```java
db.commitTransactionAsync(transaction -> {
        transaction.prepareStatement("CREATE TABLE TABLE_A (COLUMN_A varchar);")
            .executeUpdate()
            .join();

        transaction.prepareStatement("INSERT INTO TABLE_A VALUES (?);")
            .withArguments("hello")
            .executeUpdate()
            .join();
});
```
- Use `runTransactionAsync(Callback)` if you would like to explicitly commit the transaction from within the callback.
  (via `transaction.commit().join()`)
- Both `commitTransactionAsync` and `runTransactionAsync` return a `CompletableFuture`
which encapsulates the return value of the callback function.

## Gotchas
- Unlike in JDBC, indexes for statement parameters start at `0` not `1`.
- Only numerical, boolean, and varchar data types are supported.
- This library has a transitive dependency on the AWS Java SDK v2.

## Motivations
The official [Official Data API Client Library](https://github.com/awslabs/rds-data-api-client-library-java) is
  lacking in a few key categories.
  - It has a dependency on AWS SDK v1 and can not support
    CompletableFuture. 
  - The parameterization syntax is too verbose, whereas the original JDBC
    syntax is much more concise.
      - Official AWS Client Library Param Syntax: `SELECT * FROM table_a WHERE column_a = :column_a;`
      - JDBC-like Parameterization: `SELECT * FROM table_a WHERE column_a = ?;`
  - It only exposes concrete types so there is no easy way to Mock
    the API.
    

## DTO
Mapping results to POJOs is outside the scope of this project.
