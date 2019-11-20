package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.repository.CrudRepository;
import pl.edu.pw.elka.polishentitylinker.entities.AliasEntity;

public interface AliasRepository extends CrudRepository<AliasEntity, Integer> {
}
