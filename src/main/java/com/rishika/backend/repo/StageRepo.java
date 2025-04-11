package com.rishika.backend.repo;

import com.rishika.backend.entity.Pipeline;
import com.rishika.backend.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepo  extends JpaRepository<Stage, Long> {
}
