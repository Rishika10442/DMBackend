package com.rishika.backend.repo;

import com.rishika.backend.entity.Pipeline;
import com.rishika.backend.entity.PipelineX;
import com.rishika.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface PipelineRepo extends JpaRepository<Pipeline, Long> {
    List<Pipeline> findByUser(User user);
    Pipeline findByPid(Long pid);

}