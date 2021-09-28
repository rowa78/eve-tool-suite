package de.ronnywalter.eve.accountservice.repository;

import de.ronnywalter.eve.accountservice.model.EveCharacter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CharacterRepository extends MongoRepository<EveCharacter, Integer> {
    Optional<EveCharacter> findByName(String name);
}
