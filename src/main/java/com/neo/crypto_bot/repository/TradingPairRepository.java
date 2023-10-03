package com.neo.crypto_bot.repository;

import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Repository
public interface TradingPairRepository extends JpaRepository<TradingPair, Long> {

    @Query(value = "select u.favorites from users u where u.id = :userId")
    Set<TradingPair> getUsersFavoritePairs(@Param("userId") long chatId);

    @Query(value = "select p from trading_pairs  p where p.name = :name")
    Optional<TradingPair> findByName(@Param("name") String pairName);

    @Transactional
    @Modifying
    @Query(value = "update trading_pairs p set p.requests = p.requests + 1 where p.name = :name")
    void increaseRate(@Param("name") String pairName);
}
