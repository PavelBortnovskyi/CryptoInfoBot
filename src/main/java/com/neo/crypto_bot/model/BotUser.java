package com.neo.crypto_bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class BotUser {

    @Id
    @Column(name = "user_id")
    private Long id;

    private String nickName;

    private String firstName;

    private String lastName;

    private LocalDateTime registeredAt;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinTable(name = "users_favorites",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "pair_id", referencedColumnName = "pair_id"))
    private Set<TradingPair> favorites = new HashSet<>();
}
