package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;

import java.util.List;

public interface WikiItemRepository extends CrudRepository<WikiItemEntity, String> {

    @Query(value = "SELECT * FROM wiki_item WHERE LOWER(label_pl) similar to  LOWER(CONCAT('%\\y',?1,'\\y%'))" +
            " OR LOWER(title_pl) similar to  LOWER(CONCAT('%\\y',?1,'\\y%'))",
            nativeQuery = true)
    List<WikiItemEntity> findAllSimilarToLemma(@Param("lemma") String lemma);


    @Query(value = "select * from wiki_item where id in (select distinct target_id from redirect_page where LOWER(label) similar to  LOWER(CONCAT('%\\y',?1,'\\y%')))",
            nativeQuery = true)
    List<WikiItemEntity> findAllSimilarToLemmaFromRedirects(@Param("lemma") String lemma);

    WikiItemEntity findByTitlePl(String titlePl);

    @Query(value = "SELECT * FROM wiki_item WHERE LOWER(label_pl) =  LOWER(?1) OR LOWER(title_pl) =  LOWER(?1)",
            nativeQuery = true)
    List<WikiItemEntity> findAllByLemma(String lemma);

    @Query(value = "select * from wiki_item where id in (select distinct target_id from redirect_page where LOWER(label) =  LOWER(?1))",
            nativeQuery = true)
    List<WikiItemEntity> findAllByLemmaFromRedirects(@Param("lemma") String lemma);
}
