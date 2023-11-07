package com.neo.crypto_bot.repository;

import com.neo.crypto_bot.model.BotUser;
import com.neo.crypto_bot.model.TradingPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    @Transactional
    @Query("select u from BotUser u where u.favorites IS NOT EMPTY")
    List<BotUser> getUsersWithFavorites();

    @Query("SELECT u FROM BotUser u LEFT JOIN FETCH u.favorites WHERE u.id = :userId")
    BotUser getUserWithFavoritePairs(@Param("userId") Long userId);
}
