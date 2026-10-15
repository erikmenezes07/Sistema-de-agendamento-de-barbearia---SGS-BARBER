package com.sgsbarber.controller;

import com.sgsbarber.model.RatingModel;
import com.sgsbarber.service.RatingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.List;
@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @GetMapping
    public ResponseEntity<List<RatingModel>> listRatings() {
        return ResponseEntity.ok(ratingService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RatingModel> getRating(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(ratingService.findById(id));
    }

    @PostMapping
    public ResponseEntity<RatingModel> createRating(@RequestBody @NonNull RatingModel rating) {
        RatingModel saved = ratingService.create(rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RatingModel> updateRating(@PathVariable @NonNull Long id, @RequestBody @NonNull RatingModel rating) {
        return ResponseEntity.ok(ratingService.update(id, rating));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@PathVariable @NonNull Long id) { ratingService.delete(id); }
}
