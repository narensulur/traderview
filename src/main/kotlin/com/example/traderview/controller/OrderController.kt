package com.example.traderview.controller

import com.example.traderview.dto.CreateOrderRequest
import com.example.traderview.dto.OrderDto
import com.example.traderview.entity.OrderStatus
import com.example.traderview.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management operations")
class OrderController(
    private val orderService: OrderService
) {
    
    @GetMapping
    @Operation(summary = "Get all orders")
    fun getAllOrders(): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(orderService.getAllOrders())
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    fun getOrderById(@PathVariable id: Long): ResponseEntity<OrderDto> {
        val order = orderService.getOrderById(id)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/broker-order/{brokerOrderId}")
    @Operation(summary = "Get order by broker order ID")
    fun getOrderByBrokerOrderId(@PathVariable brokerOrderId: String): ResponseEntity<OrderDto> {
        val order = orderService.getOrderByBrokerOrderId(brokerOrderId)
        return if (order != null) {
            ResponseEntity.ok(order)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/trading-account/{tradingAccountId}")
    @Operation(summary = "Get orders by trading account")
    fun getOrdersByTradingAccount(@PathVariable tradingAccountId: Long): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(orderService.getOrdersByTradingAccount(tradingAccountId))
    }
    
    @GetMapping("/symbol/{symbolId}")
    @Operation(summary = "Get orders by symbol")
    fun getOrdersBySymbol(@PathVariable symbolId: Long): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(orderService.getOrdersBySymbol(symbolId))
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status")
    fun getOrdersByStatus(@PathVariable status: OrderStatus): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status))
    }
    
    @GetMapping("/date-range")
    @Operation(summary = "Get orders by date range")
    fun getOrdersByDateRange(
        @RequestParam(required = false) tradingAccountId: Long?,
        @RequestParam(required = false) symbolId: Long?,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime
    ): ResponseEntity<List<OrderDto>> {
        return ResponseEntity.ok(
            orderService.getOrdersByDateRange(tradingAccountId, symbolId, startDate, endDate)
        )
    }
    
    @PostMapping
    @Operation(summary = "Create a new order")
    fun createOrder(@Valid @RequestBody request: CreateOrderRequest): ResponseEntity<OrderDto> {
        return try {
            val order = orderService.createOrder(request)
            ResponseEntity.status(HttpStatus.CREATED).body(order)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order")
    fun deleteOrder(@PathVariable id: Long): ResponseEntity<Void> {
        return if (orderService.deleteOrder(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
