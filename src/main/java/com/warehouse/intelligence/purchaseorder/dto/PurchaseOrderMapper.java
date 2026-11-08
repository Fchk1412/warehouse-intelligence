package com.warehouse.intelligence.purchaseorder.dto;

import com.warehouse.intelligence.product.Product;
import com.warehouse.intelligence.purchaseorder.PurchaseOrder;
import com.warehouse.intelligence.purchaseorder.PurchaseOrderItem;
import com.warehouse.intelligence.supplier.Supplier;
import com.warehouse.intelligence.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.List;

public class PurchaseOrderMapper {

    public static PurchaseOrderItemDto toItemDto(PurchaseOrderItem item) {
        Product product = item.getProduct();
        BigDecimal lineTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new PurchaseOrderItemDto(
                item.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                lineTotal
        );
    }

    public static PurchaseOrderDto toPurchaseOrderDto(PurchaseOrder order) {
        Supplier supplier = order.getSupplier();
        Warehouse warehouse = order.getWarehouse();

        List<PurchaseOrderItemDto> itemDtos = order.getItems().stream()
                .map(PurchaseOrderMapper::toItemDto)
                .toList();

        BigDecimal totalAmount = itemDtos.stream()
                .map(PurchaseOrderItemDto::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchaseOrderDto(
                order.getId(),
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                order.getStatus(),
                itemDtos,
                totalAmount,
                order.getCreatedAt(),
                order.getReceivedAt()
        );
    }
}

