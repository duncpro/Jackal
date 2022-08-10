package com.duncpro.jackal

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.ByteArrayInputStream

internal class MiscKotlinSupportKtTest {

    @Test
    fun testCompileSQLScript() = runBlocking {
        val stream = ByteArrayInputStream("""
            INSERT INTO people (name) VALUES (?);
            
            DELETE FROM people WHERE name = ?;
        """.trimIndent().toByteArray())

        val actual = compileSQLScript(stream, "Duncan", "Bob").toList()
        assert(actual.size == 2)
        assert(actual[0].sql.toString() == "INSERT INTO people (name) VALUES (?);")
        assert(actual[0].args == listOf("Duncan"))
        assert(actual[1].sql.toString() == "DELETE FROM people WHERE name = ?;")
        assert(actual[1].args == listOf("Bob"))
    }
}
