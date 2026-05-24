package com.voiceinput.pro.repository;

import com.voiceinput.pro.model.entity.HotwordEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HotwordRepository extends JpaRepository<HotwordEntity, String> {

    String SEARCH_FILTER = """
        from hotword h
        join hotword_category c on c.id = h.category_id
        where (cast(:categoryId as bigint) is null or h.category_id = cast(:categoryId as bigint))
          and (cast(:keyword as text) is null
               or lower(cast(h.recognized_term as text)) like lower(concat('%', cast(:keyword as text), '%'))
               or lower(cast(h.standard_term as text)) like lower(concat('%', cast(:keyword as text), '%')))
        """;

    @Query("""
        select h from HotwordEntity h
        join fetch h.category c
        where h.enabled = true
        """)
    List<HotwordEntity> findAllEnabled();

    @Query(
        value = "select h.* " + SEARCH_FILTER + " order by c.sort_order asc, h.created_at desc",
        countQuery = "select count(*) " + SEARCH_FILTER,
        nativeQuery = true
    )
    Page<HotwordEntity> search(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    @Query("""
        select h from HotwordEntity h
        join fetch h.category c
        """)
    List<HotwordEntity> findAllWithCategory();
}
