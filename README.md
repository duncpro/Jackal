# Amazon RDS Data API Wrapper
Small wrapper around RDS Data API (AWS SDK v2).

## Getting Started
```java
final AsyncDatabase db = new AmazonRDSAsyncDatabaseWrapper(/* */);
```

## Features
### Fully embraces Java 8's CompletableFuture
All calls return `CompletableFuture` making it easy to integrate relational database queries into your asynchronous event-based application.
```java
db.prepareStatement("SELECT * FROM person")
        .executeQuery() // returns CompletableFuture<QueryResult>
        .thenApply(rs -> rs.toRowList().get(0).get("name"));
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
final AsyncDatabase db = new DataSourceAsyncDatabase();
```
### Transactions
```java
asyncDb.runTransactionAsync(transaction -> {
        transaction.prepareStatement("CREATE TABLE TABLE_A (COLUMN_A varchar);")
            .executeUpdate()
            .join();

        transaction.prepareStatement("INSERT INTO TABLE_A VALUES (?);")
            .setString(0, "hello")
            .executeUpdate()
            .join();

        transaction.commit().join();
});
```
- Use `commitTransactionAsync(Callback)` to implicitly commit the transaction upon
non-exceptional completion of the callback.
- Both `commitTransactionAsync` and `runTransactionAsync` return a `CompletableFuture`
which encapsulates the return value of the callback function.

## Gotchas
- Unlike in JDBC, indexes for statement parameters start at `0` not `1`.
- Only numerical, boolean, and varchar data types are supported.
