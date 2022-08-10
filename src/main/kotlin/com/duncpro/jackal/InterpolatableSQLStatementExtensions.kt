package com.duncpro.jackal

import kotlinx.coroutines.future.await
import java.util.stream.Stream

context(SQLExecutorProvider)
suspend fun InterpolatableSQLStatement.executeQuery(): Stream<QueryResultRow> =
    this.executeQueryAsync(this@SQLExecutorProvider).await()

context(SQLExecutorProvider)
suspend fun InterpolatableSQLStatement.executeUpdate() {
    this.executeUpdateAsync(this@SQLExecutorProvider).await()
}
