package com.example.sachitech.controller;

import com.example.sachitech.dto.CourseReportDTO;
import com.example.sachitech.dto.DashboardSummaryDTO;
import com.example.sachitech.dto.ProfitLossReportDTO;
import com.example.sachitech.dto.RevenueReportDTO;
import com.example.sachitech.dto.StudentReportDTO;
import com.example.sachitech.entity.FeeTransaction;
import com.example.sachitech.entity.StudentProfile;
import com.example.sachitech.repository.FeeRecordRepository;
import com.example.sachitech.repository.FeeTransactionRepository;
import com.example.sachitech.repository.StudentProfileRepository;
import com.example.sachitech.service.DashboardService;
import com.example.sachitech.service.TrainerPaymentService;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Provides all report endpoints consumed by the SuperAdmin dashboard.
 *
 * GET /reports/summary          → DashboardSummaryDTO (cached)
 * GET /reports/student          → List<StudentReportDTO>
 * GET /reports/revenue          → List<RevenueReportDTO>
 * GET /reports/course           → List<CourseReportDTO>
 * GET /api/reports/download/{month}/{year}?format=pdf|excel
 */
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final DashboardService          dashboardService;
    private final FeeTransactionRepository  feeTransactionRepository;
    private final StudentProfileRepository  studentProfileRepository;
    private final FeeRecordRepository       feeRecordRepository;
    private final TrainerPaymentService     trainerPaymentService;

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard data endpoints
    // ─────────────────────────────────────────────────────────────────────────
    @Cacheable("dashboardSummary")
    @GetMapping("/reports/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/reports/student")
    public ResponseEntity<List<StudentReportDTO>> getStudentReport() {
        return ResponseEntity.ok(dashboardService.getStudentReport());
    }

    @GetMapping("/reports/revenue")
    public ResponseEntity<List<RevenueReportDTO>> getRevenueReport() {
        return ResponseEntity.ok(dashboardService.getRevenueReport());
    }

    @GetMapping("/reports/course")
    public ResponseEntity<List<CourseReportDTO>> getCourseReport() {
        return ResponseEntity.ok(dashboardService.getCourseReport());
    }

    @GetMapping("/reports/profit-loss")
    public ResponseEntity<com.example.sachitech.dto.ProfitLossReportDTO> getProfitLossReport() {
        com.example.sachitech.dto.ProfitLossReportDTO report = new com.example.sachitech.dto.ProfitLossReportDTO();
        
        Double totalStudentFeesCollected = feeRecordRepository.sumTotalCollected();
        Double totalTrainerPayments = 0.0; // Will be populated if TrainerPaymentRepository is available
        
        report.setTotalStudentFeesCollected(totalStudentFeesCollected != null ? totalStudentFeesCollected : 0.0);
        report.setTotalTrainerPayments(totalTrainerPayments);
        report.setProfitLoss(report.getTotalStudentFeesCollected() - totalTrainerPayments);
        
        Double profitMargin = report.getTotalStudentFeesCollected() > 0 
            ? (report.getProfitLoss() / report.getTotalStudentFeesCollected()) * 100 
            : 0.0;
        report.setProfitMargin(profitMargin);
        
        report.setTotalStudents(studentProfileRepository.count());
        report.setTotalTrainers(0L); // Will be populated if TrainerProfileRepository is available
        report.setTotalTransactions(feeTransactionRepository.countAllTransactions());
        
        return ResponseEntity.ok(report);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Downloadable monthly report
    // GET /api/reports/download/{month}/{year}?format=pdf   (default)
    // GET /api/reports/download/{month}/{year}?format=excel
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/reports/download/{month}/{year}")
    public ResponseEntity<byte[]> downloadMonthlyReport(
            @PathVariable int month,
            @PathVariable int year,
            @RequestParam(defaultValue = "pdf") String format) throws Exception {

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        // Fetch data for the period
        List<FeeTransaction>  transactions = feeTransactionRepository.findByMonthAndYear(month, year);
        List<StudentProfile>  newAdmissions = studentProfileRepository.findByAdmissionMonthAndYear(month, year);
        double totalCollected = transactions.stream()
                .mapToDouble(FeeTransaction::getInstallmentAmount).sum();
        double totalPending   = feeRecordRepository.sumTotalPending();
        double totalExpected  = feeRecordRepository.sumTotalExpectedRevenue();

        if ("excel".equalsIgnoreCase(format)) {
            byte[] bytes = buildExcel(monthName, year, transactions, newAdmissions, totalCollected, totalPending, totalExpected);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=Monthly_Report_" + monthName + "_" + year + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        }

        // Default: PDF
        byte[] bytes = buildPdf(monthName, year, transactions, newAdmissions, totalCollected, totalPending, totalExpected);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Monthly_Report_" + monthName + "_" + year + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF builder (OpenPDF / LibrePDF)
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] buildPdf(String monthName, int year,
                             List<FeeTransaction> transactions,
                             List<StudentProfile> newAdmissions,
                             double totalCollected, double totalPending, double totalExpected) throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 60, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont  = new Font(Font.HELVETICA, 18, Font.BOLD, Color.decode("#1e3a5f"));
        Font headFont   = new Font(Font.HELVETICA, 12, Font.BOLD, Color.WHITE);
        Font bodyFont   = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font labelFont  = new Font(Font.HELVETICA, 10, Font.BOLD);

        // ── Title ──────────────────────────────────────────────────────────
        Paragraph title = new Paragraph("Monthly Report — " + monthName + " " + year, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        doc.add(title);

        Paragraph sub = new Paragraph("Generated by Sachitech Training Management System", bodyFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20);
        doc.add(sub);

        // ── Summary section ────────────────────────────────────────────────
        doc.add(new Paragraph("Financial Summary", labelFont));
        doc.add(Chunk.NEWLINE);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        summaryTable.setSpacingAfter(20);

        addSummaryRow(summaryTable, "Total Revenue Collected", "₹" + String.format("%,.2f", totalCollected), bodyFont, labelFont);
        addSummaryRow(summaryTable, "Total Pending Dues",      "₹" + String.format("%,.2f", totalPending),   bodyFont, labelFont);
        addSummaryRow(summaryTable, "Total Expected Revenue",  "₹" + String.format("%,.2f", totalExpected),  bodyFont, labelFont);
        addSummaryRow(summaryTable, "New Admissions",          String.valueOf(newAdmissions.size()),          bodyFont, labelFont);
        addSummaryRow(summaryTable, "Transactions This Month", String.valueOf(transactions.size()),           bodyFont, labelFont);
        doc.add(summaryTable);

        // ── New Admissions ─────────────────────────────────────────────────
        doc.add(new Paragraph("New Admissions — " + monthName + " " + year, labelFont));
        doc.add(Chunk.NEWLINE);

        if (newAdmissions.isEmpty()) {
            doc.add(new Paragraph("No new admissions this month.", bodyFont));
        } else {
            PdfPTable admTable = new PdfPTable(new float[]{1, 3, 3, 2});
            admTable.setWidthPercentage(100);
            admTable.setSpacingAfter(20);
            addTableHeader(admTable, headFont, "#", "Name", "Email", "Admission Date");
            int idx = 1;
            for (StudentProfile sp : newAdmissions) {
                String name  = sp.getUser() != null ? sp.getUser().getName()  : "—";
                String email = sp.getUser() != null ? sp.getUser().getEmail() : "—";
                String date  = sp.getAdmissionDate() != null ? sp.getAdmissionDate().toString() : "—";
                addTableRow(admTable, bodyFont, String.valueOf(idx++), name, email, date);
            }
            doc.add(admTable);
        }

        // ── Transaction List ───────────────────────────────────────────────
        doc.add(new Paragraph("Transaction Details — " + monthName + " " + year, labelFont));
        doc.add(Chunk.NEWLINE);

        if (transactions.isEmpty()) {
            doc.add(new Paragraph("No transactions recorded this month.", bodyFont));
        } else {
            PdfPTable txTable = new PdfPTable(new float[]{1, 3, 2, 2, 2, 2});
            txTable.setWidthPercentage(100);
            txTable.setSpacingAfter(10);
            addTableHeader(txTable, headFont, "#", "Student", "Course", "Amount (₹)", "Date", "Type");
            int idx = 1;
            for (FeeTransaction tx : transactions) {
                String student = tx.getFeeRecord() != null && tx.getFeeRecord().getStudent() != null
                        && tx.getFeeRecord().getStudent().getUser() != null
                        ? tx.getFeeRecord().getStudent().getUser().getName() : "—";
                String course  = tx.getFeeRecord() != null && tx.getFeeRecord().getCourse() != null
                        ? tx.getFeeRecord().getCourse().getName() : "—";
                addTableRow(txTable, bodyFont,
                        String.valueOf(idx++),
                        student,
                        course,
                        String.format("%,.2f", tx.getInstallmentAmount()),
                        tx.getPaymentDate().toString(),
                        tx.getTransactionType());
            }
            doc.add(txTable);
        }

        doc.close();
        return out.toByteArray();
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font bodyFont, Font labelFont) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, labelFont));
        lCell.setBorder(Rectangle.BOX);
        lCell.setPadding(6);
        lCell.setBackgroundColor(Color.decode("#f0f4f8"));
        table.addCell(lCell);

        PdfPCell vCell = new PdfPCell(new Phrase(value, bodyFont));
        vCell.setBorder(Rectangle.BOX);
        vCell.setPadding(6);
        table.addCell(vCell);
    }

    private void addTableHeader(PdfPTable table, Font headFont, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(Color.decode("#1e3a5f"));
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, Font bodyFont, String... values) {
        for (String v : values) {
            PdfPCell cell = new PdfPCell(new Phrase(v, bodyFont));
            cell.setPadding(5);
            cell.setBorder(Rectangle.BOX);
            table.addCell(cell);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Excel builder (Apache POI)
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] buildExcel(String monthName, int year,
                               List<FeeTransaction> transactions,
                               List<StudentProfile> newAdmissions,
                               double totalCollected, double totalPending, double totalExpected) throws Exception {

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── Summary sheet ──────────────────────────────────────────────
            Sheet summary = wb.createSheet("Summary");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle currencyStyle = wb.createCellStyle();
            currencyStyle.setDataFormat(wb.createDataFormat().getFormat("₹#,##0.00"));

            Row titleRow = summary.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Monthly Report — " + monthName + " " + year);
            CellStyle titleStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            String[][] summaryData = {
                    {"Metric", "Value"},
                    {"Total Revenue Collected", String.format("%.2f", totalCollected)},
                    {"Total Pending Dues",      String.format("%.2f", totalPending)},
                    {"Total Expected Revenue",  String.format("%.2f", totalExpected)},
                    {"New Admissions",          String.valueOf(newAdmissions.size())},
                    {"Transactions This Month", String.valueOf(transactions.size())},
            };
            for (int i = 0; i < summaryData.length; i++) {
                Row row = summary.createRow(i + 2);
                row.createCell(0).setCellValue(summaryData[i][0]);
                row.createCell(1).setCellValue(summaryData[i][1]);
                if (i == 0) {
                    row.getCell(0).setCellStyle(headerStyle);
                    row.getCell(1).setCellStyle(headerStyle);
                }
            }
            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);

            // ── Admissions sheet ───────────────────────────────────────────
            Sheet admSheet = wb.createSheet("New Admissions");
            String[] admHeaders = {"#", "Name", "Email", "Admission Date"};
            Row admHeader = admSheet.createRow(0);
            for (int i = 0; i < admHeaders.length; i++) {
                Cell c = admHeader.createCell(i);
                c.setCellValue(admHeaders[i]);
                c.setCellStyle(headerStyle);
            }
            int rowIdx = 1;
            for (StudentProfile sp : newAdmissions) {
                Row row = admSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(sp.getUser() != null ? sp.getUser().getName()  : "—");
                row.createCell(2).setCellValue(sp.getUser() != null ? sp.getUser().getEmail() : "—");
                row.createCell(3).setCellValue(sp.getAdmissionDate() != null ? sp.getAdmissionDate().toString() : "—");
            }
            for (int i = 0; i < admHeaders.length; i++) admSheet.autoSizeColumn(i);

            // ── Transactions sheet ─────────────────────────────────────────
            Sheet txSheet = wb.createSheet("Transactions");
            String[] txHeaders = {"#", "Student", "Course", "Amount (₹)", "Date", "Type", "Receipt No"};
            Row txHeader = txSheet.createRow(0);
            for (int i = 0; i < txHeaders.length; i++) {
                Cell c = txHeader.createCell(i);
                c.setCellValue(txHeaders[i]);
                c.setCellStyle(headerStyle);
            }
            rowIdx = 1;
            for (FeeTransaction tx : transactions) {
                Row row = txSheet.createRow(rowIdx++);
                String student = tx.getFeeRecord() != null && tx.getFeeRecord().getStudent() != null
                        && tx.getFeeRecord().getStudent().getUser() != null
                        ? tx.getFeeRecord().getStudent().getUser().getName() : "—";
                String course  = tx.getFeeRecord() != null && tx.getFeeRecord().getCourse() != null
                        ? tx.getFeeRecord().getCourse().getName() : "—";
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(student);
                row.createCell(2).setCellValue(course);
                row.createCell(3).setCellValue(tx.getInstallmentAmount());
                row.createCell(4).setCellValue(tx.getPaymentDate().toString());
                row.createCell(5).setCellValue(tx.getTransactionType());
                row.createCell(6).setCellValue(tx.getReceiptNo() != null ? tx.getReceiptNo() : "—");
            }
            for (int i = 0; i < txHeaders.length; i++) txSheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}
