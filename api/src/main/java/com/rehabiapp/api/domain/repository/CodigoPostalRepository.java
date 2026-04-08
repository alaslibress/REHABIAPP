package com.rehabiapp.api.domain.repository;

import com.rehabiapp.api.domain.entity.CodigoPostal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoPostalRepository extends JpaRepository<CodigoPostal, String> {
}
