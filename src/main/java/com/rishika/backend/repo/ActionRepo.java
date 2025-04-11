package com.rishika.backend.repo;

import com.rishika.backend.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActionRepo extends JpaRepository<Action, Long> {

    Optional<Action> findById(Long id);
}
