package com.example.traderview.service

import com.example.traderview.dto.CreateBrokerRequest
import com.example.traderview.entity.Broker
import com.example.traderview.repository.BrokerRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class BrokerServiceTest {
    
    @Mock
    private lateinit var brokerRepository: BrokerRepository
    
    @InjectMocks
    private lateinit var brokerService: BrokerService
    
    private lateinit var sampleBroker: Broker
    
    @BeforeEach
    fun setUp() {
        sampleBroker = Broker(
            id = 1L,
            name = "test_broker",
            displayName = "Test Broker",
            apiEndpoint = "https://api.testbroker.com",
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
    
    @Test
    fun `should get all brokers`() {
        // Given
        val brokers = listOf(sampleBroker)
        `when`(brokerRepository.findAll()).thenReturn(brokers)
        
        // When
        val result = brokerService.getAllBrokers()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("test_broker", result[0].name)
        assertEquals("Test Broker", result[0].displayName)
    }
    
    @Test
    fun `should get broker by id`() {
        // Given
        `when`(brokerRepository.findById(1L)).thenReturn(Optional.of(sampleBroker))
        
        // When
        val result = brokerService.getBrokerById(1L)
        
        // Then
        assertNotNull(result)
        assertEquals("test_broker", result?.name)
        assertEquals("Test Broker", result?.displayName)
    }
    
    @Test
    fun `should return null when broker not found`() {
        // Given
        `when`(brokerRepository.findById(999L)).thenReturn(Optional.empty())
        
        // When
        val result = brokerService.getBrokerById(999L)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `should create broker successfully`() {
        // Given
        val request = CreateBrokerRequest(
            name = "new_broker",
            displayName = "New Broker",
            apiEndpoint = "https://api.newbroker.com"
        )
        
        `when`(brokerRepository.findByName("new_broker")).thenReturn(null)
        `when`(brokerRepository.save(any(Broker::class.java))).thenReturn(
            sampleBroker.copy(name = "new_broker", displayName = "New Broker")
        )
        
        // When
        val result = brokerService.createBroker(request)
        
        // Then
        assertEquals("new_broker", result.name)
        assertEquals("New Broker", result.displayName)
        verify(brokerRepository).save(any(Broker::class.java))
    }
    
    @Test
    fun `should throw exception when creating broker with existing name`() {
        // Given
        val request = CreateBrokerRequest(
            name = "existing_broker",
            displayName = "Existing Broker",
            apiEndpoint = "https://api.existing.com"
        )
        
        `when`(brokerRepository.findByName("existing_broker")).thenReturn(sampleBroker)
        
        // When & Then
        assertThrows(IllegalArgumentException::class.java) {
            brokerService.createBroker(request)
        }
    }
}
