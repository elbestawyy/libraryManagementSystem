package com.project.springbootlibrary.dao;

import com.project.springbootlibrary.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Payment findByuserEmail(String userEmail);
}
