package pl.edu.pw.elka.polishentitylinker.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pl.edu.pw.elka.polishentitylinker.entities.WikiItemEntity;

import java.util.List;
import java.util.Optional;

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

    @Query(value = "select * from wiki_item where id in (select distinct target_id from alias where label = ?1)",
            nativeQuery = true)
    List<WikiItemEntity> findAllByOriginalFormFromAliases(@Param("originalForm") String originalForm);

    Optional<WikiItemEntity> findByPageId(Integer pageId);

    @Query(value = "select * from wiki_item where article_length is not null and page_type = 0 order by article_length desc limit ?1",
            nativeQuery = true)
    List<WikiItemEntity> findLongestArticles(int limit);

    @Query(value = "select * from wiki_item_instance_of left join wiki_item wi on wiki_item_instance_of.wiki_item_id " +
            "= wi.id where instance_of_id = ?1 order by wi.id",
            nativeQuery = true)
    List<WikiItemEntity> findByInstanceOfIsContaining(String categoryId, Pageable pageable);

    @Query(value = "select * from wiki_item_subclass_of left join wiki_item wi on wiki_item_subclass_of.wiki_item_id " +
            "= wi.id where subclass_of_id = ?1 order by wi.id",
            nativeQuery = true)
    List<WikiItemEntity> findBySubclassOfIsContaining(String categoryId, Pageable pageable);

    @Query(value = "select count(*) from wiki_item_instance_of where instance_of_id = ?1",
            nativeQuery = true)
    Integer countAllByInstanceOfIsContaining(String categoryId);

    @Query(value = "select count(*) from wiki_item_subclass_of where subclass_of_id = ?1",
            nativeQuery = true)
    Integer countAllBySubclassOfIsContaining(String categoryId);

    @Query(value = "select * from wiki_item where article_length > ?1 and page_type = 0  limit ?2",
            nativeQuery = true)
    List<WikiItemEntity> findWithHigherArticleLength(int minLength, int limit);
}
