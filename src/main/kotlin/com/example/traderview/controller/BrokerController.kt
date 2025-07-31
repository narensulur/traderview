package com.example.traderview.controller

import com.example.traderview.dto.BrokerDto
import com.example.traderview.dto.CreateBrokerRequest
import com.example.traderview.dto.UpdateBrokerRequest
import com.example.traderview.service.BrokerService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/brokers")
@Tag(name = "Brokers", description = "Broker management operations")
class BrokerController(
    private val brokerService: BrokerService
) {
    
    @GetMapping
    @Operation(summary = "Get all brokers")
    fun getAllBrokers(): ResponseEntity<List<BrokerDto>> {
        return ResponseEntity.ok(brokerService.getAllBrokers())
    }
    
    @GetMapping("/active")
    @Operation(summary = "Get all active brokers")
    fun getActiveBrokers(): ResponseEntity<List<BrokerDto>> {
        return ResponseEntity.ok(brokerService.getActiveBrokers())
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get broker by ID")
    fun getBrokerById(@PathVariable id: Long): ResponseEntity<BrokerDto> {
        val broker = brokerService.getBrokerById(id)
        return if (broker != null) {
            ResponseEntity.ok(broker)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/name/{name}")
    @Operation(summary = "Get broker by name")
    fun getBrokerByName(@PathVariable name: String): ResponseEntity<BrokerDto> {
        val broker = brokerService.getBrokerByName(name)
        return if (broker != null) {
            ResponseEntity.ok(broker)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping
    @Operation(summary = "Create a new broker")
    fun createBroker(@Valid @RequestBody request: CreateBrokerRequest): ResponseEntity<BrokerDto> {
        return try {
            val broker = brokerService.createBroker(request)
            ResponseEntity.status(HttpStatus.CREATED).body(broker)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update broker")
    fun updateBroker(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateBrokerRequest
    ): ResponseEntity<BrokerDto> {
        val broker = brokerService.updateBroker(id, request)
        return if (broker != null) {
            ResponseEntity.ok(broker)
        } else {
            ResponseEntity.notFound().build()
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete broker")
    fun deleteBroker(@PathVariable id: Long): ResponseEntity<Void> {
        return if (brokerService.deleteBroker(id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
