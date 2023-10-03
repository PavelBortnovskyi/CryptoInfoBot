package com.neo.crypto_bot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity(name = "users")
@NoArgsConstructor
public class BotUser {

    @Id
    private Long id;

    private String nickName;

    private String firstName;

    private String lastName;

    private LocalDateTime registeredAt;

    @OneToMany(fetch = FetchType.EAGER)
    private Set<TradingPair> favorites = new HashSet<>();
}
