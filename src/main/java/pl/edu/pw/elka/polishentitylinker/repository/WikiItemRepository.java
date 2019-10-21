package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.repository.CrudRepository;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;

public interface WikiItemRepository extends CrudRepository<WikiItemEntity, String> {
}
