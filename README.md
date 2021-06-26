# Amazon RDS Data API Wrapper
![Build Status](https://travis-ci.com/duncpro/rds-data-api-wrapper.svg?branch=master)

Wrapper around RDS Data API (AWS SDK v2) inspired by the minimalistic standard JDBC API.

## Getting Started
```java
final AsyncDatabase db = new AmazonRDSAsyncDatabaseWrapper(/* */);
```

## Features
### Fully embraces Java 8's CompletableFuture
All updates return `CompletableFuture` and all queries return `Stream`.
```java
asyncDb.prepareStatement("SELECT * FROM person")
        .executeQuery()
        .map(row -> row.getString("first_name"))
        .collect(Collectors.toSet());
```
### JDBC-like Parameterization
```java
db.prepareStatement("INSERT INTO person VALUES (?, ?, ?);")
        .setString(0, "Duncan Proctor")
        .setLong(1, 23)
        .setBoolean(2, true)
        .executeUpdate()
        .join();
```
### JDBC DataSource Implementation
In addition to `AmazonRDSAsyncDatabaseWrapper`, this library includes a second implementation of `AsyncDatabase`, 
called `DataSourceAsyncDatabase`. This class wraps a standard JDBC `DataSource` with `AsyncDatabase`
making it easy to test database-related code locally.
```java
final AsyncDatabase db = new DataSourceAsyncDatabase(/* */);
```
### Transactions
```java
asyncDb.commitTransactionAsync(transaction -> {
        transaction.prepareStatement("CREATE TABLE TABLE_A (COLUMN_A varchar);")
            .executeUpdate()
            .join();

        transaction.prepareStatement("INSERT INTO TABLE_A VALUES (?);")
            .setString(0, "hello")
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
      - AWS Client Library Param Syntax: `SELECT * FROM table_a WHERE column_a = :column_a;`
      - JDBC-like Syntax: `SELECT * FROM table_a WHERE column_a = ?;`
  - It only exposes concrete types so there is no easy way to Mock
    the API.
