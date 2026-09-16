package com.finshot.remittance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entitas JPA yang merepresentasikan tabel `customers` di database.
 * Sesuai data awal dari Finshot (Part 1.4): C001 (Kim), C002 (Lee), C003 (Park).
 */
@Entity
@Table(name = "customers")
public class Customer {

    /**
     * customer_id: Primary Key tabel customers (contoh: "C001").
     */
    @Id
    @Column(name = "customer_id", length = 10)
    private String customerId;

    /**
     * name: Nama nasabah (contoh: "Kim").
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Constructor kosong wajib ada untuk Hibernate/JPA
    public Customer() {
    }

    public Customer(String customerId, String name) {
        this.customerId = customerId;
        this.name = name;
    }

    // Getter dan Setter
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
