package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.repository.CrudRepository;
import pl.edu.pw.elka.polishentitylinker.entity.RedirectPageEntity;
import pl.edu.pw.elka.polishentitylinker.entity.WikiItemEntity;

import java.util.List;

public interface RedirectPageRepository extends CrudRepository<RedirectPageEntity, Integer> {

    List<RedirectPageEntity> findAllByTarget(WikiItemEntity wikiItemEntity);
}
