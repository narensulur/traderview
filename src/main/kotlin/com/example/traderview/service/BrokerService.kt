package com.example.traderview.service

import com.example.traderview.dto.BrokerDto
import com.example.traderview.dto.CreateBrokerRequest
import com.example.traderview.dto.UpdateBrokerRequest
import com.example.traderview.entity.Broker
import com.example.traderview.repository.BrokerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class BrokerService(
    private val brokerRepository: BrokerRepository
) {
    
    fun getAllBrokers(): List<BrokerDto> {
        return brokerRepository.findAll().map { it.toDto() }
    }
    
    fun getActiveBrokers(): List<BrokerDto> {
        return brokerRepository.findByIsActiveTrue().map { it.toDto() }
    }
    
    fun getBrokerById(id: Long): BrokerDto? {
        return brokerRepository.findById(id).orElse(null)?.toDto()
    }
    
    fun getBrokerByName(name: String): BrokerDto? {
        return brokerRepository.findByName(name)?.toDto()
    }
    
    fun createBroker(request: CreateBrokerRequest): BrokerDto {
        val existingBroker = brokerRepository.findByName(request.name)
        if (existingBroker != null) {
            throw IllegalArgumentException("Broker with name '${request.name}' already exists")
        }
        
        val broker = Broker(
            name = request.name,
            displayName = request.displayName,
            apiEndpoint = request.apiEndpoint,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return brokerRepository.save(broker).toDto()
    }
    
    fun updateBroker(id: Long, request: UpdateBrokerRequest): BrokerDto? {
        val broker = brokerRepository.findById(id).orElse(null) ?: return null
        
        val updatedBroker = broker.copy(
            displayName = request.displayName ?: broker.displayName,
            apiEndpoint = request.apiEndpoint ?: broker.apiEndpoint,
            isActive = request.isActive ?: broker.isActive,
            updatedAt = LocalDateTime.now()
        )
        
        return brokerRepository.save(updatedBroker).toDto()
    }
    
    fun deleteBroker(id: Long): Boolean {
        return if (brokerRepository.existsById(id)) {
            brokerRepository.deleteById(id)
            true
        } else {
            false
        }
    }
    
    private fun Broker.toDto() = BrokerDto(
        id = id,
        name = name,
        displayName = displayName,
        apiEndpoint = apiEndpoint,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
