package com.example.sachitech.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Each row is one installment / payment event.
 * Many transactions belong to one FeeRecord.
 */
@Entity
@Table(name = "fee_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_record_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private FeeRecord feeRecord;

    @Column(name = "installment_amount", nullable = false)
    private Double installmentAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    private Double amount;
    private String paymentMode;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "receipt_no", unique = true)
    private String receiptNo;
}
