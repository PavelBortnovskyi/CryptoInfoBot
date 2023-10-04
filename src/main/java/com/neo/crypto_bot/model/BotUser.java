package com.neo.crypto_bot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "users")
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
