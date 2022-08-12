package com.duncpro.jackal

import com.duncpro.jackal.InterpolatableSQLStatement.sql
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.LinkedList
import java.util.stream.Stream

enum class LoopResult { CONTINUE, BREAK }

private inline fun loop(action: () -> LoopResult) {
    var isNotBroken = true
    while (isNotBroken) {
        if (action() == LoopResult.BREAK) isNotBroken = false
    }
}

/**
 * Incrementally reads a multi-statement SQL script from the given [inputStream], and compiles each statement,
 * interpolating the given arguments into the script parameters. This function closes the given [inputStream]
 * once the last statement has been consumed from the flow.
 */
@Throws(IOException::class)
fun compileSQLScript(inputStream: InputStream, vararg scriptArgs: String): Flow<InterpolatableSQLStatement> =
    flow {
        val scriptArgsQueue = LinkedList<String>()
        scriptArgs.forEach { scriptArgsQueue.offer(it) }

        InputStreamReader(inputStream).use { reader ->
            var c: Int

            @Suppress("SqlDialectInspection", "SqlNoDataSourceInspection")
            var buffer = ""
            var paramCount = 0

            loop {
                c = reader.read()

                if (c == -1) return@loop LoopResult.BREAK
                buffer += c.toChar()

                when (c.toChar()) {
                    '?' -> { paramCount += 1 }
                    ';' -> {
                        val statementArgs = Array<String?>(paramCount) { null }
                        repeat(paramCount) { i -> statementArgs[i] = scriptArgsQueue.remove() }
                        emit(sql(buffer.trim()).withArguments(*statementArgs))
                        buffer = ""
                        paramCount = 0
                    }
                }

                return@loop LoopResult.CONTINUE
            }
        }
    }.flowOn(Dispatchers.IO)

/**
 * Starts a new asynchronous SQL transaction which will be automatically closed once the given [block]
 * of code terminates. The [block] should explicitly call [AsyncSQLTransaction.commitAsync] if the effects
 * of the transaction should be preserved. If the aforementioned function is not called by the time the
 * [block] terminates, the transaction will be rolled back. If the [block] throws an exception before
 * [AsyncSQLTransaction.commitAsync] is invoked then the transaction will be rolled back.
 */
suspend inline fun <T> SQLDatabase.executeTransaction(block: AsyncSQLTransactionContext.() -> T): T {
    val transaction = this.startTransactionAsync().await()
    try {
        return with(AsyncSQLTransactionContext(transaction)) { block() }
    } finally {
        transaction.closeAsync().await()
    }
}

class AsyncSQLTransactionContext constructor(private val transaction: AsyncSQLTransaction) {
    /**
     * Executes the update represented by this [InterpolatableSQLStatement] in the context of the [AsyncSQLTransaction]
     * represented by the current scope. This function is a more ergonomic shortcut to
     * [InterpolatableSQLStatement.executeUpdate].
     */
    suspend fun InterpolatableSQLStatement.executeUpdate() {
        executeUpdateAsync(transaction).await()
    }

    /**
     * Executes the query represented by this [InterpolatableSQLStatement] in the context of the [AsyncSQLTransaction]
     * represented by the current scope. This function is a more ergonomic shortcut to
     * [InterpolatableSQLStatement.executeQuery].
     */
    suspend fun InterpolatableSQLStatement.executeQuery(): Stream<QueryResultRow> {
        return executeQueryAsync(transaction).await()
    }

    /**
     * Commits the ongoing [AsyncSQLTransaction] which is represented by the current scope or block of code.
     * This function is a more ergonomic shortcut to [AsyncSQLTransaction.commitAsync].
     */
    suspend fun commit() {
        transaction.commitAsync().await()
    }
}