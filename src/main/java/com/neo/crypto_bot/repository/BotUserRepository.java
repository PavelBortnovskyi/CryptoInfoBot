package com.neo.crypto_bot.repository;

import com.neo.crypto_bot.model.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotUserRepository extends JpaRepository<BotUser, Long> {
}
