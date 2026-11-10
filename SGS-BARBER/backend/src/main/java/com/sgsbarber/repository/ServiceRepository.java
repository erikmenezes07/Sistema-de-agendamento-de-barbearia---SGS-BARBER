package com.sgsbarber.repository;

import com.sgsbarber.model.ServiceModel;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ServiceRepository extends JpaRepository<ServiceModel, Long> {
}
