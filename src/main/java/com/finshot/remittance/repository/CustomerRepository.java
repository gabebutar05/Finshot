package com.finshot.remittance.repository;

import com.finshot.remittance.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data Access Layer untuk entitas Customer (Part 1 & Part 2).
 *
 * Di C#/.NET: Setara dengan DbContext.Customers (DbSet<Customer>) atau ICustomerRepository.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Mengambil customer sekaligus melakukan Pessimistic Write Lock (SELECT ... FOR UPDATE).
     *
     * Kegunaan krusial:
     * Mencegah race condition ketika dua request transfer datang bersamaan untuk customer yang sama.
     * Lock ini menjamin pengecekan limit harian (5.000.000 KRW) dieksekusi secara serial (satu per satu),
     * sehingga customer tidak bisa membobol limit harian melalui concurrent requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Customer c WHERE c.customerId = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") String id);
}
