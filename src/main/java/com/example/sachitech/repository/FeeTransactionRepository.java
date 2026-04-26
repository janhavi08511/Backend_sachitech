package com.example.sachitech.repository;

import com.example.sachitech.entity.FeeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FeeTransactionRepository extends JpaRepository<FeeTransaction, Long> {

    List<FeeTransaction> findByFeeRecordId(Long feeRecordId);

    List<FeeTransaction> findByFeeRecordStudentId(Long studentId);

    /** All transactions for a student, ordered newest first */
    List<FeeTransaction> findByFeeRecordStudentIdOrderByPaymentDateDesc(Long studentId);

    /** Admin: all transactions in a date range */
    @Query("SELECT t FROM FeeTransaction t WHERE t.paymentDate BETWEEN :from AND :to ORDER BY t.paymentDate DESC")
    List<FeeTransaction> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Admin: transactions by student name (partial match) */
    @Query("SELECT t FROM FeeTransaction t WHERE LOWER(t.feeRecord.student.user.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY t.paymentDate DESC")
    List<FeeTransaction> findByStudentNameContaining(@Param("name") String name);

    /** Sum collected this month */
    @Query("SELECT COALESCE(SUM(t.installmentAmount), 0) FROM FeeTransaction t WHERE YEAR(t.paymentDate) = :year AND MONTH(t.paymentDate) = :month")
    Double sumCollectedInMonth(@Param("year") int year, @Param("month") int month);

    /** Total transaction count */
    @Query("SELECT COUNT(t) FROM FeeTransaction t")
    Long countAllTransactions();

    /**
     * Monthly revenue trend for a given year.
     * Returns Object[] rows: [monthNumber(1-12), monthName, totalCollected]
     */
    @Query(value = """
        SELECT
            MONTH(t.payment_date)                    AS monthNum,
            DATE_FORMAT(t.payment_date, '%b')        AS monthName,
            COALESCE(SUM(t.installment_amount), 0)   AS collected
        FROM fee_transaction t
        WHERE YEAR(t.payment_date) = :year
        GROUP BY MONTH(t.payment_date), DATE_FORMAT(t.payment_date, '%b')
        ORDER BY MONTH(t.payment_date)
        """, nativeQuery = true)
    List<Object[]> findMonthlyRevenue(@Param("year") int year);

    /**
     * All transactions for a given month/year — used in the downloadable report.
     */
    @Query("SELECT t FROM FeeTransaction t WHERE YEAR(t.paymentDate) = :year AND MONTH(t.paymentDate) = :month ORDER BY t.paymentDate DESC")
    List<FeeTransaction> findByMonthAndYear(@Param("month") int month, @Param("year") int year);
}
