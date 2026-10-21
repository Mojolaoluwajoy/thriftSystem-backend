package com.thrift.thriftsystem.repository;

import com.thrift.thriftsystem.model.Currency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends MongoRepository<Currency, String> {

    Optional<Currency> findByCode(String code);

    List<Currency> findByActiveTrue();

    boolean existsByCode(String code);
}
