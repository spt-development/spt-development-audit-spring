package com.spt.development.audit.spring;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionSyncManFacade {

    boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    void register(TransactionSynchronization synchronization) {
        TransactionSynchronizationManager.registerSynchronization(synchronization);
    }
}
