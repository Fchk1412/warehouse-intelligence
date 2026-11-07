package com.warehouse.intelligence.movement;

import com.warehouse.intelligence.inventory.Inventory;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false, updatable = false)
    private Inventory inventory;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20, updatable = false)
    private MovementType movementType;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(length = 255, updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InventoryMovement() {
    }

    public InventoryMovement(Inventory inventory, MovementType movementType, int quantity, String reason) {
        this.inventory = inventory;
        this.movementType = movementType;
        this.quantity = quantity;
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

