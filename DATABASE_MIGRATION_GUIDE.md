# Database Migration Guide - SachITech Training Management System

## Overview

This guide provides step-by-step instructions for applying database schema changes needed for:
1. Fee Management System (Task 9)
2. Trainer Payment Management
3. Profit/Loss Reporting
4. Trainer Dashboard Real Data

---

## New Tables Required

### 1. fee_record
Stores fee information for each student-course enrollment.

```sql
CREATE TABLE fee_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  course_id BIGINT,
  total_fee_at_enrollment DOUBLE NOT NULL,
  amount_paid DOUBLE DEFAULT 0,
  pending_amount DOUBLE NOT NULL,
  last_transaction_date DATE,
  version BIGINT,
  FOREIGN KEY (student_id) REFERENCES student_profile(id),
  FOREIGN KEY (course_id) REFERENCES course(id)
);
```

**Purpose**: Tracks total fees, amount paid, and pending amount for each student-course pair.

**Key Fields**:
- `total_fee_at_enrollment`: Snapshot of course fee at enrollment time (immutable)
- `amount_paid`: Running total of all payments received
- `pending_amount`: Auto-calculated (total_fee - amount_paid)
- `version`: For optimistic locking (prevents concurrent update conflicts)

---

### 2. fee_transaction
Records individual payment transactions.

```sql
CREATE TABLE fee_transaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  fee_record_id BIGINT NOT NULL,
  installment_amount DOUBLE NOT NULL,
  payment_date DATE NOT NULL,
  transaction_type VARCHAR(50),
  receipt_no VARCHAR(100) UNIQUE,
  FOREIGN KEY (fee_record_id) REFERENCES fee_record(id)
);
```

**Purpose**: Maintains audit trail of all fee payments.

**Key Fields**:
- `transaction_type`: CASH, ONLINE, CHEQUE
- `receipt_no`: Unique receipt number (format: RCP-{timestamp}-{UUID})

---

### 3. trainer_payment
Records payments made to trainers.

```sql
CREATE TABLE trainer_payment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trainer_id BIGINT NOT NULL,
  amount DOUBLE NOT NULL,
  payment_mode VARCHAR(50),
  payment_reference VARCHAR(100) UNIQUE NOT NULL,
  payment_date DATE NOT NULL,
  status VARCHAR(50),
  remarks VARCHAR(500),
  created_at DATE NOT NULL,
  updated_at DATE NOT NULL,
  FOREIGN KEY (trainer_id) REFERENCES trainer_profile(id)
);
```

**Purpose**: Tracks all payments made to trainers for profit/loss calculation.

**Key Fields**:
- `payment_mode`: CASH, ONLINE, CHEQUE
- `status`: PENDING, COMPLETED, CANCELLED
- `payment_reference`: Unique reference (format: TRP-{timestamp}-{UUID})

---

## Column Additions to Existing Tables

### course table
```sql
ALTER TABLE course ADD COLUMN total_fee DOUBLE DEFAULT 0;
ALTER TABLE course ADD COLUMN name VARCHAR(255);
ALTER TABLE course ADD COLUMN trainer_id BIGINT;
ALTER TABLE course ADD FOREIGN KEY (trainer_id) REFERENCES trainer_profile(id);
```

### student_profile table
```sql
ALTER TABLE student_profile ADD COLUMN phone VARCHAR(20);
ALTER TABLE student_profile ADD COLUMN course VARCHAR(255);
ALTER TABLE student_profile ADD COLUMN admission_date DATE;
ALTER TABLE student_profile ADD COLUMN fee_paid BOOLEAN DEFAULT FALSE;
```

### trainer_profile table
```sql
ALTER TABLE trainer_profile ADD COLUMN phone VARCHAR(20);
```

### assignment table
```sql
ALTER TABLE assignment ADD COLUMN title VARCHAR(255);
ALTER TABLE assignment ADD COLUMN max_score DOUBLE;
ALTER TABLE assignment ADD COLUMN score DOUBLE;
ALTER TABLE assignment ADD COLUMN evaluation_status VARCHAR(50);
```

---

## Junction Tables for Many-to-Many Relationships

### student_courses
```sql
CREATE TABLE student_courses (
  student_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  PRIMARY KEY (student_id, course_id),
  FOREIGN KEY (student_id) REFERENCES student_profile(id),
  FOREIGN KEY (course_id) REFERENCES course(id)
);
```

### student_internships
```sql
CREATE TABLE student_internships (
  student_id BIGINT NOT NULL,
  internship_id BIGINT NOT NULL,
  PRIMARY KEY (student_id, internship_id),
  FOREIGN KEY (student_id) REFERENCES student_profile(id),
  FOREIGN KEY (internship_id) REFERENCES internship(id)
);
```

### trainer_courses
```sql
CREATE TABLE trainer_courses (
  trainer_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  PRIMARY KEY (trainer_id, course_id),
  FOREIGN KEY (trainer_id) REFERENCES trainer_profile(id),
  FOREIGN KEY (course_id) REFERENCES course(id)
);
```

### trainer_internships
```sql
CREATE TABLE trainer_internships (
  trainer_id BIGINT NOT NULL,
  internship_id BIGINT NOT NULL,
  PRIMARY KEY (trainer_id, internship_id),
  FOREIGN KEY (trainer_id) REFERENCES trainer_profile(id),
  FOREIGN KEY (internship_id) REFERENCES internship(id)
);
```

---

## Migration Steps

### Option 1: Using SQL Script (Recommended)

1. **Backup your database**
   ```bash
   mysqldump -u username -p tms_db > tms_db_backup.sql
   ```

2. **Run the migration script**
   ```bash
   mysql -u username -p tms_db < schema_updates.sql
   ```

3. **Verify the changes**
   ```sql
   SHOW TABLES;
   DESCRIBE fee_record;
   DESCRIBE fee_transaction;
   DESCRIBE trainer_payment;
   ```

### Option 2: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your database
3. Open `schema_updates.sql` file
4. Execute the script
5. Verify tables are created

### Option 3: Using Spring Boot JPA (Automatic)

If `spring.jpa.hibernate.ddl-auto=update` is set in `application.yml`:
1. The tables will be created automatically on application startup
2. Existing data will be preserved
3. New columns will be added to existing tables

---

## Verification Checklist

After migration, verify:

- [ ] `fee_record` table exists with all columns
- [ ] `fee_transaction` table exists with all columns
- [ ] `trainer_payment` table exists with all columns
- [ ] `course` table has `total_fee`, `name`, `trainer_id` columns
- [ ] `student_profile` table has `phone`, `course`, `admission_date`, `fee_paid` columns
- [ ] `trainer_profile` table has `phone` column
- [ ] `assignment` table has `title`, `max_score`, `score`, `evaluation_status` columns
- [ ] All junction tables exist (`student_courses`, `student_internships`, `trainer_courses`, `trainer_internships`)
- [ ] All indexes are created
- [ ] Foreign key relationships are intact

---

## Rollback Plan

If you need to rollback:

1. **Restore from backup**
   ```bash
   mysql -u username -p tms_db < tms_db_backup.sql
   ```

2. **Or manually drop new tables**
   ```sql
   DROP TABLE IF EXISTS trainer_payment;
   DROP TABLE IF EXISTS fee_transaction;
   DROP TABLE IF EXISTS fee_record;
   DROP TABLE IF EXISTS trainer_internships;
   DROP TABLE IF EXISTS trainer_courses;
   DROP TABLE IF EXISTS student_internships;
   DROP TABLE IF EXISTS student_courses;
   ```

3. **Remove added columns** (if needed)
   ```sql
   ALTER TABLE course DROP COLUMN IF EXISTS total_fee;
   ALTER TABLE course DROP COLUMN IF EXISTS name;
   ALTER TABLE course DROP COLUMN IF EXISTS trainer_id;
   -- ... etc for other tables
   ```

---

## Data Migration (If Existing Data)

If you have existing data, you may need to:

1. **Populate fee_record from existing fee_management**
   ```sql
   INSERT INTO fee_record (student_id, course_id, total_fee_at_enrollment, amount_paid, pending_amount)
   SELECT student_id, course_id, fee_amount, (fee_amount - pending_amount), pending_amount
   FROM fee_management;
   ```

2. **Populate fee_transaction from existing payments**
   ```sql
   INSERT INTO fee_transaction (fee_record_id, installment_amount, payment_date, transaction_type)
   SELECT fr.id, fm.fee_amount, fm.payment_date, fm.payment_type
   FROM fee_management fm
   JOIN fee_record fr ON fm.student_id = fr.student_id AND fm.course_id = fr.course_id;
   ```

---

## Performance Optimization

The migration includes indexes for:
- Student-course lookups
- Payment date ranges
- Status filtering
- Attendance tracking
- Assignment evaluation

These indexes improve query performance for:
- Fetching student fee records
- Generating profit/loss reports
- Trainer payment queries
- Dashboard data loading

---

## Testing After Migration

1. **Test Fee Management**
   - Create a fee record
   - Add a transaction
   - Verify pending amount calculation

2. **Test Trainer Payments**
   - Record a trainer payment
   - Verify payment reference is unique
   - Check profit/loss calculation

3. **Test Trainer Dashboard**
   - Verify real data is fetched
   - Check metrics calculation
   - Confirm performance module works

4. **Test Performance**
   - Run dashboard queries
   - Check query execution time
   - Verify indexes are being used

---

## Troubleshooting

### Issue: Foreign key constraint fails
**Solution**: Ensure referenced tables exist and have data

### Issue: Duplicate key error
**Solution**: Check for duplicate values in unique columns (receipt_no, payment_reference)

### Issue: Column already exists
**Solution**: The ALTER TABLE ADD COLUMN IF NOT EXISTS will skip existing columns

### Issue: Slow queries after migration
**Solution**: Run `ANALYZE TABLE` to update index statistics
```sql
ANALYZE TABLE fee_record;
ANALYZE TABLE fee_transaction;
ANALYZE TABLE trainer_payment;
```

---

## Support

For issues or questions:
1. Check the FEE_MANAGEMENT_SYSTEM.md documentation
2. Review the schema_updates.sql file
3. Check database logs for errors
4. Verify all foreign key relationships

---

## Summary

✅ 3 new tables created
✅ 4 existing tables updated with new columns
✅ 4 junction tables for many-to-many relationships
✅ Performance indexes added
✅ Foreign key constraints maintained
✅ Backward compatible with existing data

The database is now ready for the Fee Management System and Trainer Dashboard real data features!
