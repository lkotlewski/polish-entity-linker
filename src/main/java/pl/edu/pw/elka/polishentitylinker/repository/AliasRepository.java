package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.repository.CrudRepository;
import pl.edu.pw.elka.polishentitylinker.entity.AliasEntity;

public interface AliasRepository extends CrudRepository<AliasEntity, Integer> {
}
