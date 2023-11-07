package com.neo.crypto_bot.repository;

import com.neo.crypto_bot.model.TradingPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface TradingPairRepository extends JpaRepository<TradingPair, Long> {

    @Query(value = "select p from TradingPair p where p.name = :name")
    Optional<TradingPair> findByName(@Param("name") String pairName);

    @Transactional
    @Modifying
    @Query(value = "update TradingPair p set p.requests = p.requests + 1 where p.name = :name")
    void increaseRate(@Param("name") String pairName);

    @Transactional
    @Modifying
    @Query(value = "update TradingPair p set p.lastCurrency = :price where p.name = :name")
    void updatePrice(@Param("price") Double price, @Param("name") String pairName);

    @Query(value = "select * from trading_pairs p order by p.requests DESC limit 25", nativeQuery = true)
    List<TradingPair> getPopularPairs();

    @Query(value = "select p from TradingPair p where p.baseAsset = :assetName")
    List<TradingPair> getConvertibleAssets(@Param("assetName") String baseAssetName);
}
