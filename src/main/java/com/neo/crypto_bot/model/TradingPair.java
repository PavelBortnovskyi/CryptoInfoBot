package com.neo.crypto_bot.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "trading_pairs")
@NoArgsConstructor
public class TradingPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String baseAsset;

    private String quoteAsset;

    //private String lastCurrency; for scheduled notification sed in case more than 5% change

    private long requests;
}
