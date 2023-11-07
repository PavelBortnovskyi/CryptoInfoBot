package com.neo.crypto_bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "trading_pairs")
@NoArgsConstructor
public class TradingPair {

    @Id
    @Column(name = "pair_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String baseAsset;

    private String quoteAsset;

    private Double lastCurrency;

    private long requests;

    @ManyToMany(mappedBy = "favorites", fetch = FetchType.LAZY)
    private Set<BotUser> users = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradingPair that = (TradingPair) o;
        return id.equals(that.id) && name.equals(that.name) && baseAsset.equals(that.baseAsset) && quoteAsset.equals(that.quoteAsset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, baseAsset, quoteAsset);
    }
}
