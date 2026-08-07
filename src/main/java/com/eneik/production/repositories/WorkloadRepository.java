package com.eneik.production.repositories;

import com.eneik.production.models.Workload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkloadRepository extends JpaRepository<Workload, Long> {
}
