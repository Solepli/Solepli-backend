package com.ilta.solepli.domain.category.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ilta.solepli.domain.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  boolean existsByName(String name);

  Optional<Category> findByName(String name);
}
