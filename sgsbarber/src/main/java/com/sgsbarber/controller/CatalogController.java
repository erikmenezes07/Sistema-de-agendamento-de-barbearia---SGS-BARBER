package com.sgsbarber.controller;

import com.sgsbarber.model.*;
import com.sgsbarber.repository.*;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class CatalogController {
 private final ProductRepository products; private final WaitlistRepository waitlist; private final NotificationRepository notifications; private final BusinessProfileRepository profiles; private final UserRepository users;
 public CatalogController(ProductRepository products, WaitlistRepository waitlist, NotificationRepository notifications, BusinessProfileRepository profiles, UserRepository users){this.products=products;this.waitlist=waitlist;this.notifications=notifications;this.profiles=profiles;this.users=users;}
 @GetMapping("/products") public List<ProductModel> products(){return products.findAll();}
 @GetMapping("/products/{id}") public ProductModel product(@PathVariable @NonNull Long id){return products.findById(id).orElseThrow();}
 @PostMapping("/products") public ResponseEntity<ProductModel> product(@RequestBody @NonNull ProductModel x){return ResponseEntity.status(HttpStatus.CREATED).body(products.save(x));}
 @PutMapping("/products/{id}") public ProductModel product(@PathVariable @NonNull Long id,@RequestBody @NonNull ProductModel x){x.setId(id);return products.save(x);}
 @DeleteMapping("/products/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteProduct(@PathVariable @NonNull Long id){products.deleteById(id);}
 @GetMapping("/waitlist") public List<WaitlistModel> waitlist(){return waitlist.findAll();}
 @GetMapping("/waitlist/{id}") public WaitlistModel wait(@PathVariable @NonNull Long id){return waitlist.findById(id).orElseThrow();}
 @PostMapping("/waitlist") public ResponseEntity<WaitlistModel> wait(@RequestBody @NonNull WaitlistModel x){return ResponseEntity.status(HttpStatus.CREATED).body(waitlist.save(x));}
 @PutMapping("/waitlist/{id}") public WaitlistModel wait(@PathVariable @NonNull Long id,@RequestBody @NonNull WaitlistModel x){x.setId(id);return waitlist.save(x);}
 @DeleteMapping("/waitlist/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteWait(@PathVariable @NonNull Long id){waitlist.deleteById(id);}
 @GetMapping("/notifications") public List<NotificationModel> notifications(){return notifications.findAll();}
 @PostMapping("/notifications") public ResponseEntity<NotificationModel> notification(@RequestBody @NonNull NotificationModel x){return ResponseEntity.status(HttpStatus.CREATED).body(notifications.save(x));}
 @PutMapping("/notifications/{id}") public NotificationModel notification(@PathVariable @NonNull Long id,@RequestBody @NonNull NotificationModel x){x.setId(id);return notifications.save(x);}
 @GetMapping("/businessProfile") public List<BusinessProfileModel> profile(){return profiles.findAll();}
 @PostMapping("/businessProfile") public ResponseEntity<BusinessProfileModel> profile(@RequestBody @NonNull BusinessProfileModel x){return ResponseEntity.status(HttpStatus.CREATED).body(profiles.save(x));}
 @PutMapping("/businessProfile/{id}") public BusinessProfileModel profile(@PathVariable @NonNull Long id,@RequestBody @NonNull BusinessProfileModel x){x.setId(id);return profiles.save(x);}
 @GetMapping("/users") public List<UserModel> users(){return users.findAll();}
 @GetMapping("/users/{id}") public UserModel user(@PathVariable @NonNull Long id){return users.findById(id).orElseThrow();}
 @PostMapping("/users") public ResponseEntity<UserModel> user(@RequestBody @NonNull UserModel x){return ResponseEntity.status(HttpStatus.CREATED).body(users.save(x));}
 @PutMapping("/users/{id}") public UserModel user(@PathVariable @NonNull Long id,@RequestBody @NonNull UserModel x){x.setId(id);return users.save(x);}
}
