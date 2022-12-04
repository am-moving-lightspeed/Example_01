package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.model.Event;
import org.example.model.Order;
import org.example.repository.OrderRepository;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class EventListeningClass {

  private final OrderDealService orderDealService;
  private final OrderService orderService;
  private final OrderRepository orderRepository;
  private final ResultReplicationAvoidableProcessor<List<Order>> processor;

  // other

  @EventListener
  public void listenEvent(Event event) {
    // other logic

    processor.process(this::processEvent, this::saveOrders, this::filterFunction);
  }

  private List<Order> processEvent() {
    var orders = orderService.updateOrderStatuses();
    orderDealService.createDeals();

    // other stuff

    return orders;
  }

  private void saveOrders(List<Order> orders) {
    // other stuff

    orderRepository.saveAll(orders);
  }

  private List<Order> filterFunction(
      List<Order> updatedOrders, List<List<Order>> alreadyProcessedOrders) {
    var ordersToSubtract = alreadyProcessedOrders.stream()
        .flatMap(List::stream).toList();
    return updatedOrders.stream()
        .filter(order -> !ordersToSubtract.contains(order))
        .collect(Collectors.toList());
  }
}
