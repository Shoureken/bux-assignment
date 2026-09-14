package com.bux.investmentplans.readmodel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanReadRepository extends JpaRepository<PlanProjection, String> {

    /**
     * Keyset page of Active plans due on {@code day}, ordered by id so a
     * page always resumes past the last id already dispatched — the basis
     * for the Execution Dispatcher's bounded scan (1.5). {@code afterId}
     * is empty for the first page of the day.
     */
    @Query("""
            select p from PlanProjection p
            where p.status = 'ACTIVE' and p.recurrenceDay = :day
            and (:afterId is null or p.id > :afterId)
            order by p.id asc
            """)
    List<PlanProjection> findDuePlansPage(@Param("day") int day, @Param("afterId") Optional<String> afterId,
                                           org.springframework.data.domain.Pageable pageable);
}
