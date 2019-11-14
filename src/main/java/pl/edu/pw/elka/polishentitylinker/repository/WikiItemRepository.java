package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;

import java.util.List;

public interface WikiItemRepository extends CrudRepository<WikiItemEntity, String> {

    @Query(value = "SELECT * FROM wiki_item WHERE LOWER(label_pl) LIKE LOWER(CONCAT('%',?1,'%'))" +
            " OR LOWER(title_pl) LIKE LOWER(CONCAT('%',?1,'%'))",
            nativeQuery = true)
    List<WikiItemEntity> findAllByLemma(@Param("lemma") String lemma);

    WikiItemEntity findByTitlePl(String titlePl);
}
