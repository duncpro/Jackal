# Jackal
[![Build Status](https://travis-ci.com/duncpro/jackal.svg?branch=master)](https://travis-ci.com/duncpro/jackal)
[![codecov](https://codecov.io/gh/duncpro/jackal/branch/master/graph/badge.svg?token=B5MZD14GUT)](https://codecov.io/gh/duncpro/jackal)
[![](https://jitpack.io/v/com.duncpro/jackal.svg)](https://jitpack.io/#com.duncpro/jackal)

Abstraction over RDS Aurora Data API and JDBC for Java 8.
Run your RDS Aurora Data API dependent applications locally using JDBC. 

## Overview
### Using Jackal with the Aurora Data API
In production and staging environments your application will likely be using
a real Aurora database. `AuroraServerlessDatabase` is an implementation of `SQLDatabase` which wraps
the Aurora Data API Client included in AWS SDK v2.
```java
final SQLDatabase db = new AuroraServerlessDatabase(/* */);
```
### Using Jackal with JDBC
In development/testing scenarios you're likely using a locally hosted or in-memory database.
Jackal provides a second implementation of `SQLException`
which wraps a standard JDBC `DataSource`. 
```java
final SQLDatabase db = new DataSourceWrapper(/* */);
```
### Async Updates using CompletableFuture
Since the Aurora Data API is built on top of AWS SDK v2 and therefore Netty, performance can be improved by executing 
updates asynchronously.
```java
import static com.duncpro.jackal.InterpolatableSQLStatement.sql;

final CompletableFuture<Void> u1 = sql("INSERT INTO person VALUES (?, ?, ?);")
            .withArguments("Duncan Proctor", 23, true)
            .executeUpdateAsync(db);

final CompletableFuture<Void> u2 = /* .. */
        
CompletableFuture.allOf(u1, u2);
```
### Blocking Updates
`InterpolatableSQLStatement#executeUpdate` is an alternative to the aforementioned method which blocks
the current thread until the update has completed. 

### Queries Using Java 8's Stream
`InterpolatableSQLStatement#executeQuery` returns a `Stream` which makes processing result sets much more ergonomic than
traditional JDBC. This function prefetches all results and closes any resources associated with the query.
```java
final Set<String> firstNames = sql("SELECT first_name FROM person LIMIT 10;")
        .executeQuery(db)
        .map(row -> row.get("first_name", String.class))
        .map(Optional::orElseThrow) // first_name is a NOT NULL column
        .collect(Collectors.toSet())
```
There is a second variant to this function, `executeQueryAsync` which returns a `CompletableFuture`. This is useful for 
performing queries concurrently. 

There is a third variant for his function: `executeQueryIncrementally` which does not prefetch results. Instead results
are fetched on an as-needed basis. This variant should be used for potentially
large query result sets. Not all client implementations support this method and therefore
it should only be used when absolutely necessary. The JDBC implementation supports this method, but
the RDS Aurora implementation will not, and will therefore throw `UnsupportedOperationException`.
The stream returned from this method is *resourceful* and must be closed explicitly with a call to
`Stream#close` or implicity with a try-with-resources block.
### JDBC-like Parameterization
Jackal supports statement parameterization using JDBC-like syntax.

```java
sql("INSERT INTO person VALUES (?, ?, ?);")
        .withArguments("Duncan Proctor", 23, true)
        .executeUpdate(db)
```

### Blocking Transaction API
Transactions must be explicitly committed using the `commit` method.
Rollbacks however are implicit. If `SQLTransaction#close` is called before
`SQLTransaction#commit`, like in the case of a mid-transaction exception, then
the transaction will be automatically rolled back.

All methods of `SQLTransaction` block the current thread.
```java
try (final var transaction = db.startTransaction()) {
    sql("INSERT INTO Person VALUES (?);")
        .withArguments("Bob")
        .executeUpdate(transaction);
    transaction.commit();
} catch (com.duncpro.jackal.SQLException e) {
    e.printStackTrace();
}
```

### Suspending Transaction API
When using Kotlin it is possible to perform SQL transactions without blocking a platform thread.
This is accomplished using Kotlin coroutines. The Suspending Transaction API is similar to
the Blocking Transaction API in that commits are explicit and rollbacks are implicit.
```kotlin
val database: SQLDatabase = TODO()
database.executeTransaction {
    sql("INSERT INTO Person VALUES (?);")
        .withArguments("Bob")
        .executeUpdate() // The executeUpdate() suspending extension receives SQLExecutorProvider
                         // provider as a context-parameter. SQLDatabase.executeTransaction invokes
                         // the given lambda function with an SQLExecutorProvider in the context.
    commit()
}
```
### Exceptions
`com.duncpro.jackal.SQLException` serves as an abstraction over Java's `SQLException` and RDS's `SdkException`.
All Jackal functions throw `com.duncpro.jackal.SQLException` but the underlying platform-specific exception
can still be accessed via `Exception#getCause` if necessary.

## Motivation
The [Official RDS Data API Client Library](https://github.com/awslabs/rds-data-api-client-library-java) is
  lacking in a few key categories.
  - It has a dependency on AWS SDK v1 and can not support
    CompletableFuture. 
  - The parameterization syntax is too verbose, whereas the original JDBC
    syntax is much more concise.
  - It provides no facilities for local testing/development.

## Jackal's Drawbacks
This library prioritizes ergonomics, simplicity, and correctness, over memory efficiency and CPU efficiency.
Using Jackal will result in a higher memory and CPU footprint when compared to raw JDBC
or Aurora calls. As such this library may not be suitable for use in projects where extremely low-latency is
necessary. For most cases, including typical web applications, this latency should be negligible.
If performance is a critical component of your application, consider using a different library, or a different
programming language altogether.
