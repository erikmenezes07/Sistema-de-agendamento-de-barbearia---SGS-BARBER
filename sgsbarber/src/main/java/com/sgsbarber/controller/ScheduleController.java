package com.sgsbarber.controller;

import com.sgsbarber.model.*;
import com.sgsbarber.repository.*;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class ScheduleController {
 private final BlockRepository blocks; private final BarberScheduleRepository schedules; private final BarberTimeOffRepository timeOff;
 public ScheduleController(BlockRepository blocks,BarberScheduleRepository schedules,BarberTimeOffRepository timeOff){this.blocks=blocks;this.schedules=schedules;this.timeOff=timeOff;}
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
 @GetMapping("/barberTimeOff") public List<BarberTimeOffModel> timeOff(){return timeOff.findAll();}
 @GetMapping("/barberTimeOff/{id}") public BarberTimeOffModel timeOff(@PathVariable @NonNull Long id){return timeOff.findById(id).orElseThrow();}
 @PostMapping("/barberTimeOff") public ResponseEntity<BarberTimeOffModel> timeOff(@RequestBody @NonNull BarberTimeOffModel x){return ResponseEntity.status(HttpStatus.CREATED).body(timeOff.save(x));}
 @PutMapping("/barberTimeOff/{id}") public BarberTimeOffModel timeOff(@PathVariable @NonNull Long id,@RequestBody @NonNull BarberTimeOffModel x){x.setId(id);return timeOff.save(x);}
 @DeleteMapping("/barberTimeOff/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteTimeOff(@PathVariable @NonNull Long id){timeOff.deleteById(id);}
}
