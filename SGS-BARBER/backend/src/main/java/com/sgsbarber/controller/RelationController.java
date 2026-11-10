package com.sgsbarber.controller;
import com.sgsbarber.model.*; import com.sgsbarber.repository.*; import org.springframework.http.*; import org.springframework.lang.NonNull; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api") public class RelationController {
 private final BarberServiceLinkRepository links; private final AppointmentProductRepository items;
 public RelationController(BarberServiceLinkRepository links,AppointmentProductRepository items){this.links=links;this.items=items;}
 @GetMapping("/barberServices") public List<BarberServiceLinkModel> links(){return links.findAll();}
 @PostMapping("/barberServices") public ResponseEntity<BarberServiceLinkModel> link(@RequestBody @NonNull BarberServiceLinkModel x){return ResponseEntity.status(HttpStatus.CREATED).body(links.save(x));}
 @DeleteMapping("/barberServices/{barberId}/{serviceId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void unlink(@PathVariable @NonNull Long barberId,@PathVariable @NonNull Long serviceId){links.deleteById(new BarberServiceLinkId(barberId,serviceId));}
 @GetMapping("/appointmentProducts") public List<AppointmentProductModel> items(){return items.findAll();}
 @GetMapping("/appointmentProducts/{id}") public AppointmentProductModel item(@PathVariable @NonNull Long id){return items.findById(id).orElseThrow();}
 @PostMapping("/appointmentProducts") public ResponseEntity<AppointmentProductModel> item(@RequestBody @NonNull AppointmentProductModel x){return ResponseEntity.status(HttpStatus.CREATED).body(items.save(x));}
 @PutMapping("/appointmentProducts/{id}") public AppointmentProductModel item(@PathVariable @NonNull Long id,@RequestBody @NonNull AppointmentProductModel x){x.setId(id);return items.save(x);}
 @DeleteMapping("/appointmentProducts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteItem(@PathVariable @NonNull Long id){items.deleteById(id);}
}
