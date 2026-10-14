package com.sgsbarber.repository;

import com.sgsbarber.model.BlockModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface BlockRepository extends JpaRepository<BlockModel, Long> {
    List<BlockModel> findByDate(LocalDate date);
}
