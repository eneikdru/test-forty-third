package com.eneik.production.repositories;

import com.eneik.production.models.VisibilityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisibilityRuleRepository extends JpaRepository<VisibilityRule, Long> {
}
