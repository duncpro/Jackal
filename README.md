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
save on AWS costs, and develop in a more transparent environment.

Jackal provides a second implementation of `RelationalDatabase`
which wraps a standard JDBC `DataSource`. 
```java
final RelationalDatabase db = new DataSourceWrapper(/* */);
```
### Parallel Updates using CompletableFuture
Since the Aurora Data API is built on top of AWS SDK v2 and therefore HTTP, performance can be improved by running updates in parallel. 
`SQLStatementBuilder#executeUpdate` returns a `CompletableFuture` making it easy to perform multiple updates
simultaneously.
```java
final CompletableFuture<Void> u1 = db.prepareStatement("INSERT INTO person VALUES (?, ?, ?);")
        .withArguments("Duncan Proctor", 23, true)
        .executeUpdate();

final CompletableFuture<Void> u2 = /* .. */
        
CompletableFuture.allOf(u1, u2);
```
### Process Queries Using Java 8's Stream
`SQLStatementBuilder#executeQuery` returns a `Stream` which makes processing result sets much more ergonomic than
traditional JDBC.
```java
final Set<String> firstNames = new HashSet<String>;
try (final var results = db.prepareStatement("SELECT first_name FROM person")
        .executeQuery()) {
        results
            .map(row -> row.get("first_name", String.class))
            .map(Optional::orElseThrow) // first_name is a NOT NULL column
            .forEach(firstNames::add);
}
```
It's worth noting that example above is blocking code. Parallelizing queries can also improve application performance, so consider wrapping this in a 
`CompletableFuture.supplyAsync` if you wish to run multiple queries simultaniously.

### JDBC-like Parameterization
Jackal supports statement parameterization using JDBC-like syntax.

```java
db.prepareStatement("INSERT INTO person VALUES (?, ?, ?);")
        .withArguments("Duncan Proctor", 23, true)
        .executeUpdate()
```
### Transactions
Jackal exposes Aurora Data API's transaction support using a callback-style interface inspired by the
JavaScript Firebase Admin API.

First create a class to represent your transaction.
```java
class InsertAndGetYoungestTransaction implements Function<TransactionHandle, Person> {
    private final int age;
    InsertAndGetYoungestTransaction(int age) {
        this.age = age;
    }
    
    @Override
    public Person apply(TransactionHandle th) {
        th.prepareStatement("INSERT INTO Person (age INT);")
                .withArgument(age)
                .executeUpdate()
                .join();

        try (final var result = th.prepareStatement("SELECT * FROM PERSON SORT BY age ASC;")
                .executeQuery()) {
            
            return result.map(Person::fromRow).findFirst()
                    .orElseThrow(AssertionError::new);
        }
    }
}
```
Now instantiate your transaction and commit it to the database.
```java
final CompletableFuture<Person> youngest = 
        db.commitTransaction(new InsertAndGetYoungestTransaction(26));
```
If your transaction's `apply` function throws an exception, the database transaction will
be rolled back, and the exception propagated into the returned CompletableFuture.

Alternatively use `runTransactionAsync(Function<TransactionHandle, T>)` if you would like to explicitly finalize the
transaction from within the callback via `TransactionHandle#commit()` and `TransactionHandle#rollback()`.


## Motivation
The [Official Data API Client Library](https://github.com/awslabs/rds-data-api-client-library-java) is
  lacking in a few key categories.
  - It has a dependency on AWS SDK v1 and can not support
    CompletableFuture. 
  - The parameterization syntax is too verbose, whereas the original JDBC
    syntax is much more concise.
  - It provides no facilities for local testing/development.
