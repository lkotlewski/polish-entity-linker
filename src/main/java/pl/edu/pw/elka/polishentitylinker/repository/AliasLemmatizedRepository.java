package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.repository.CrudRepository;
import pl.edu.pw.elka.polishentitylinker.entities.AliasLemmatizedEntity;

public interface AliasLemmatizedRepository extends CrudRepository<AliasLemmatizedEntity, Integer> {
}
