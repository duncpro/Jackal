package com.duncpro.jackal

import kotlinx.coroutines.future.await

/**
 * Starts a new asynchronous SQL transaction which will be automatically closed once the given [block]
 * of code terminates. The [block] should explicitly call [AsyncSQLTransaction.commit] if the effects
 * of the transaction should be preserved. If the aforementioned function is not called by the time the
 * [block] terminates, the transaction will be rolled back. If the [block] throws an exception before
 * [AsyncSQLTransaction.commit] is invoked then the transaction will be rolled back.
 */
suspend inline fun <T> SQLDatabase.executeTransaction(block: AsyncSQLTransaction.() -> T): T {
    val transaction = this.startTransactionAsync().await()
    try {
        return with(transaction) { block() }
    } finally {
        transaction.close()
    }
}
