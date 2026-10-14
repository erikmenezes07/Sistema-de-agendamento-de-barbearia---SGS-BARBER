package com.sgsbarber.controller;

import com.sgsbarber.model.*;
import com.sgsbarber.repository.*;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class ScheduleController {
 private final BlockRepository blocks; private final BarberScheduleRepository schedules;
 public ScheduleController(BlockRepository blocks,BarberScheduleRepository schedules){this.blocks=blocks;this.schedules=schedules;}
 @GetMapping("/blocks") public List<BlockModel> blocks(){return blocks.findAll();}
 @GetMapping("/blocks/{id}") public BlockModel block(@PathVariable @NonNull Long id){return blocks.findById(id).orElseThrow();}
 @PostMapping("/blocks") public ResponseEntity<BlockModel> block(@RequestBody @NonNull BlockModel x){return ResponseEntity.status(HttpStatus.CREATED).body(blocks.save(x));}
 @PutMapping("/blocks/{id}") public BlockModel block(@PathVariable @NonNull Long id,@RequestBody @NonNull BlockModel x){x.setId(id);return blocks.save(x);}
 @DeleteMapping("/blocks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteBlock(@PathVariable @NonNull Long id){blocks.deleteById(id);}
 @GetMapping("/barberSchedules") public List<BarberScheduleModel> schedules(){return schedules.findAll();}
 @GetMapping("/barberSchedules/{id}") public BarberScheduleModel schedule(@PathVariable @NonNull Long id){return schedules.findById(id).orElseThrow();}
 @PostMapping("/barberSchedules") public ResponseEntity<BarberScheduleModel> schedule(@RequestBody @NonNull BarberScheduleModel x){return ResponseEntity.status(HttpStatus.CREATED).body(schedules.save(x));}
 @PutMapping("/barberSchedules/{id}") public BarberScheduleModel schedule(@PathVariable @NonNull Long id,@RequestBody @NonNull BarberScheduleModel x){x.setId(id);return schedules.save(x);}
 @DeleteMapping("/barberSchedules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSchedule(@PathVariable @NonNull Long id){schedules.deleteById(id);}
}
