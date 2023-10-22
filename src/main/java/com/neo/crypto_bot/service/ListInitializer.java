package com.neo.crypto_bot.service;

import com.neo.crypto_bot.model.TradingPair;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListInitializer {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveEntitiesInBatch(List<TradingPair> entities) {
        int batchSize = 100;

        for (int i = 0; i < entities.size(); i++) {
            TradingPair entity = entities.get(i);
            entityManager.persist(entity);

            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}

