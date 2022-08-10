package com.duncpro.jackal

import kotlinx.coroutines.future.await

suspend fun AsyncSQLTransaction.commit() { this.commitAsync().await() }
