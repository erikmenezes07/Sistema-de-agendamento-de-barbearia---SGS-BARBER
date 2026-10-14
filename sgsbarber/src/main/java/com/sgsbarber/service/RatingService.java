package com.sgsbarber.service;

import com.sgsbarber.model.RatingModel;
import com.sgsbarber.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.util.List;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public List<RatingModel> listAll() {
        return ratingRepository.findAll();
    }

    public RatingModel findById(@NonNull Long id) { return ratingRepository.findById(id).orElseThrow(); }

    public RatingModel create(@NonNull RatingModel rating) {
        return ratingRepository.save(rating);
    }

    public RatingModel update(@NonNull Long id, @NonNull RatingModel rating) {
        rating.setId(id);
        return ratingRepository.save(rating);
    }

    public void delete(@NonNull Long id) { ratingRepository.deleteById(id); }
}
