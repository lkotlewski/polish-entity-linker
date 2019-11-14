package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.repository.CrudRepository;
import pl.edu.pw.elka.polishentitylinker.entities.RedirectPageEntity;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;

import java.util.List;

public interface RedirectPageRepository extends CrudRepository<RedirectPageEntity, Integer> {

    List<RedirectPageEntity> findAllByTarget(WikiItemEntity wikiItemEntity);
}
