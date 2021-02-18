package com.spt.development.audit.spring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class TransactionSyncManFacadeTest {

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.clear();
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void isTransactionActive_default_shouldReturnFalse() {
        final boolean result = createFacade().isTransactionActive();

        assertThat(result, is(false));
    }

    @Test
    void isTransactionActive_transactionActive_shouldReturnTrue() {
        TransactionSynchronizationManager.setActualTransactionActive(true);

        final boolean result = createFacade().isTransactionActive();

        assertThat(result, is(true));
    }

    @Test
    void register_validSync_shouldRegisterSync() {
        final TransactionSynchronization sync = Mockito.mock(TransactionSynchronization.class);

        createFacade().register(sync);

        assertThat(TransactionSynchronizationManager.getSynchronizations().contains(sync), is(true));
    }

    private TransactionSyncManFacade createFacade() {
        return new TransactionSyncManFacade();
    }
}