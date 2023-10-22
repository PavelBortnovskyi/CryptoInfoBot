package com.neo.crypto_bot.repository;

import com.neo.crypto_bot.model.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BotUserRepository extends JpaRepository<BotUser, Long> {

    @Transactional
    @Query("select u from BotUser u where u.favorites IS NOT EMPTY")
    List<BotUser> getUsersWithFavorites();
}
