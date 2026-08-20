package com.example.expancemanager.data

/**
 * Runs a block of DAO calls inside a single database transaction so multi-table
 * writes are atomic (all-or-nothing). Abstracting this behind an interface keeps
 * data/domain classes decoupled from Room's `withTransaction` and makes them
 * unit-testable (tests supply a runner that executes the block inline).
 */
internal fun interface TransactionRunner {
    suspend operator fun invoke(block: suspend () -> Unit)
}
