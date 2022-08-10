package com.duncpro.jackal

import com.duncpro.jackal.InterpolatableSQLStatement.sql
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.LinkedList

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
