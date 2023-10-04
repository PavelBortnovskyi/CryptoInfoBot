package com.neo.crypto_bot.repository;

import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    @Query(value = "select u.favorites from BotUser u where u.id = :userId")
    Set<TradingPair> getUsersFavoritePairs(@Param("userId") long chatId);
}
