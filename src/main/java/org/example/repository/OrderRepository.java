package org.example.repository;

import org.example.model.Order;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository {

  void saveAll(List<Order> orders);
}
