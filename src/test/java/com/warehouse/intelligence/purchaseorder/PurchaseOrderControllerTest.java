package com.warehouse.intelligence.purchaseorder;

import com.warehouse.intelligence.common.exception.BusinessRuleException;
import com.warehouse.intelligence.common.exception.ResourceNotFoundException;
import com.warehouse.intelligence.purchaseorder.dto.CreatePurchaseOrderRequest;
import com.warehouse.intelligence.purchaseorder.dto.PurchaseOrderDto;
import com.warehouse.intelligence.purchaseorder.dto.PurchaseOrderItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseOrderController.class)
class PurchaseOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseOrderService purchaseOrderService;

    private PurchaseOrderDto sampleOrderDto(PurchaseOrderStatus status, Instant receivedAt) {
        PurchaseOrderItemDto item = new PurchaseOrderItemDto(
                1L,
                10L,
                "LAPTOP-001",
                "Lenovo ThinkPad T14",
                20,
                new BigDecimal("1100.00"),
                new BigDecimal("22000.00")
        );

        return new PurchaseOrderDto(
                50L,
                1L,
                "SUP-LENOVO",
                "Lenovo Italy S.r.l.",
                2L,
                "MOD-01",
                "Modena Central Warehouse",
                status,
                List.of(item),
                new BigDecimal("22000.00"),
                Instant.parse("2026-11-08T10:00:00Z"),
                receivedAt
        );
    }

    @Test
    void shouldReturnPagedPurchaseOrders() throws Exception {
        // Arrange
        Page<PurchaseOrderDto> page = new PageImpl<>(
                List.of(sampleOrderDto(PurchaseOrderStatus.PENDING, null))
        );
        when(purchaseOrderService.getAllPurchaseOrders(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        // Act + Assert
        mockMvc.perform(get("/api/purchase-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(50))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(22000.00));
    }

    @Test
    void shouldReturnPurchaseOrderByIdWhenExists() throws Exception {
        // Arrange
        when(purchaseOrderService.getPurchaseOrderById(50L))
                .thenReturn(sampleOrderDto(PurchaseOrderStatus.PENDING, null));

        // Act + Assert
        mockMvc.perform(get("/api/purchase-orders/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.supplierCode").value("SUP-LENOVO"))
                .andExpect(jsonPath("$.warehouseCode").value("MOD-01"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productSku").value("LAPTOP-001"))
                .andExpect(jsonPath("$.totalAmount").value(22000.00));
    }

    @Test
    void shouldReturn404WhenPurchaseOrderDoesNotExist() throws Exception {
        // Arrange
        when(purchaseOrderService.getPurchaseOrderById(999L))
                .thenThrow(new ResourceNotFoundException("Purchase order not found"));

        // Act + Assert
        mockMvc.perform(get("/api/purchase-orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Purchase order not found"));
    }

    @Test
    void shouldCreatePurchaseOrderAndReturn201() throws Exception {
        // Arrange
        when(purchaseOrderService.createPurchaseOrder(any(CreatePurchaseOrderRequest.class)))
                .thenReturn(sampleOrderDto(PurchaseOrderStatus.PENDING, null));

        String requestBody = """
                {
                    "supplierId": 1,
                    "warehouseId": 2,
                    "items": [
                        {
                            "productId": 10,
                            "quantity": 20
                        }
                    ]
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(22000.00));
    }

    @Test
    void shouldReturn400WhenCreatePurchaseOrderHasEmptyItems() throws Exception {
        // Arrange
        String invalidBody = """
                {
                    "supplierId": 1,
                    "warehouseId": 2,
                    "items": []
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/api/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.items").exists());

        verify(purchaseOrderService, never()).createPurchaseOrder(any());
    }

    @Test
    void shouldApprovePurchaseOrderAndReturn200() throws Exception {
        // Arrange
        when(purchaseOrderService.approvePurchaseOrder(50L))
                .thenReturn(sampleOrderDto(PurchaseOrderStatus.APPROVED, null));

        // Act + Assert
        mockMvc.perform(post("/api/purchase-orders/50/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void shouldCancelPurchaseOrderAndReturn200() throws Exception {
        // Arrange
        when(purchaseOrderService.cancelPurchaseOrder(50L))
                .thenReturn(sampleOrderDto(PurchaseOrderStatus.CANCELLED, null));

        // Act + Assert
        mockMvc.perform(post("/api/purchase-orders/50/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReceivePurchaseOrderAndReturn200() throws Exception {
        // Arrange
        when(purchaseOrderService.receivePurchaseOrder(50L))
                .thenReturn(sampleOrderDto(
                        PurchaseOrderStatus.RECEIVED,
                        Instant.parse("2026-11-08T12:00:00Z")
                ));

        // Act + Assert
        mockMvc.perform(post("/api/purchase-orders/50/receive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.receivedAt").value("2026-11-08T12:00:00Z"));
    }

    @Test
    void shouldReturn409WhenReceivingPendingPurchaseOrder() throws Exception {
        // Arrange
        when(purchaseOrderService.receivePurchaseOrder(50L))
                .thenThrow(new BusinessRuleException("Only APPROVED purchase orders can be received (current status: PENDING)"));

        // Act + Assert
        mockMvc.perform(post("/api/purchase-orders/50/receive"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

