package com.tenantsphere.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs best-effort side effects (mail, file cleanup) once the data is committed. A failing side
 * effect is logged instead of thrown: the request already succeeded, and an error response would
 * invite the client to retry and create a duplicate.
 */
public final class AfterCommit {
    private static final Logger LOGGER = LoggerFactory.getLogger(AfterCommit.class);

    private AfterCommit() {}

    public static void run(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runSafely(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runSafely(action);
            }
        });
    }

    private static void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            LOGGER.warn("A post-commit side effect failed; the committed change is kept.", exception);
        }
    }
}
