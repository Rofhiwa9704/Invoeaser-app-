package co.za.kingstechco.kingstechco.invoeaserapp.util;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.InvoiceItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InvoiceCalculationUtilTest {

    private Invoice testInvoice;
    private List<InvoiceItem> testItems;

    @BeforeEach
    void setUp() {
        testInvoice = new Invoice();
        testItems = new ArrayList<>();
        
        // Item 1: 10 hours @ $50/hour = $500
        InvoiceItem item1 = new InvoiceItem();
        item1.setInvoiceItemId(1L);
        item1.setHoursWorked(10);
        item1.setHourlyRate(BigDecimal.valueOf(50.00));
        testItems.add(item1);
        
        // Item 2: 5 quantity @ $75/unit = $375
        InvoiceItem item2 = new InvoiceItem();
        item2.setInvoiceItemId(2L);
        item2.setQuantity(5);
        item2.setUnitPrice(BigDecimal.valueOf(75.00));
        testItems.add(item2);
        
        testInvoice.setInvoiceItems(new HashSet<>(testItems));
    }

    @Test
    void calculateItemLineTotal_ShouldCalculateCorrectTotal_ForHourlyRate() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setHoursWorked(10);
        item.setHourlyRate(BigDecimal.valueOf(50.00));
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertEquals(BigDecimal.valueOf(500.00), item.getLineTotal());
    }

    @Test
    void calculateItemLineTotal_ShouldCalculateCorrectTotal_ForUnitPrice() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setQuantity(5);
        item.setUnitPrice(BigDecimal.valueOf(75.00));
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertEquals(BigDecimal.valueOf(375.00), item.getLineTotal());
    }

    @Test
    void calculateItemLineTotal_ShouldNotCalculate_WhenHourlyRateIsNull() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setHoursWorked(10);
        item.setHourlyRate(null);
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertNull(item.getLineTotal());
    }

    @Test
    void calculateItemLineTotal_ShouldNotCalculate_WhenHoursWorkedIsNull() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setHoursWorked(null);
        item.setHourlyRate(BigDecimal.valueOf(50.00));
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertNull(item.getLineTotal());
    }

    @Test
    void calculateItemLineTotal_ShouldNotCalculate_WhenUnitPriceIsNull() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setQuantity(5);
        item.setUnitPrice(null);
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertNull(item.getLineTotal());
    }

    @Test
    void calculateItemLineTotal_ShouldNotCalculate_WhenQuantityIsNull() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setQuantity(null);
        item.setUnitPrice(BigDecimal.valueOf(75.00));
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertNull(item.getLineTotal());
    }

    @Test
    void calculateInvoiceTotal_ShouldCalculateCorrectTotal() {
        // Given
        // Pre-calculate line totals for test items
        testItems.get(0).setLineTotal(BigDecimal.valueOf(500.00));
        testItems.get(1).setLineTotal(BigDecimal.valueOf(375.00));
        
        // When
        InvoiceCalculationUtil.calculateInvoiceTotal(testInvoice);
        
        // Then
        assertEquals(BigDecimal.valueOf(875.00), testInvoice.getTotalAmount());
    }

    @Test
    void calculateInvoiceTotal_ShouldCalculateTotal_WithItemCalculation() {
        // When
        InvoiceCalculationUtil.calculateInvoiceTotal(testInvoice);
        
        // Then
        // Verify line totals were calculated
        assertEquals(BigDecimal.valueOf(500.00), testItems.get(0).getLineTotal());
        assertEquals(BigDecimal.valueOf(375.00), testItems.get(1).getLineTotal());
        
        // Verify total was calculated
        assertEquals(BigDecimal.valueOf(875.00), testInvoice.getTotalAmount());
    }

    @Test
    void calculateInvoiceTotal_ShouldHandleNullLineTotals() {
        // Given
        InvoiceItem itemWithNullTotal = new InvoiceItem();
        itemWithNullTotal.setInvoiceItemId(3L);
        itemWithNullTotal.setLineTotal(null);
        testItems.add(itemWithNullTotal);
        testInvoice.setInvoiceItems(new HashSet<>(testItems));
        
        // Pre-calculate line totals for test items
        testItems.get(0).setLineTotal(BigDecimal.valueOf(500.00));
        testItems.get(1).setLineTotal(BigDecimal.valueOf(375.00));
        
        // When
        InvoiceCalculationUtil.calculateInvoiceTotal(testInvoice);
        
        // Then
        assertEquals(BigDecimal.valueOf(875.00), testInvoice.getTotalAmount());
    }

    @Test
    void calculateInvoiceTotal_ShouldReturnZero_WhenNoItems() {
        // Given
        Invoice emptyInvoice = new Invoice();
        emptyInvoice.setInvoiceItems(new HashSet<>());
        
        // When
        InvoiceCalculationUtil.calculateInvoiceTotal(emptyInvoice);
        
        // Then
        assertEquals(0, emptyInvoice.getTotalAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateInvoiceTotal_ShouldReturnZero_WhenAllItemsHaveNullLineTotals() {
        // Given
        Invoice invoiceWithNullTotals = new Invoice();
        List<InvoiceItem> itemsWithNullTotals = new ArrayList<>();
        
        InvoiceItem item1 = new InvoiceItem();
        item1.setLineTotal(null);
        itemsWithNullTotals.add(item1);
        
        InvoiceItem item2 = new InvoiceItem();
        item2.setLineTotal(null);
        itemsWithNullTotals.add(item2);
        
        invoiceWithNullTotals.setInvoiceItems(new HashSet<>(itemsWithNullTotals));
        
        // When
        InvoiceCalculationUtil.calculateInvoiceTotal(invoiceWithNullTotals);
        
        // Then
        assertEquals(0, invoiceWithNullTotals.getTotalAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateItemLineTotal_ShouldPrioritizeHourlyRate_WhenBothRatesPresent() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setHoursWorked(10);
        item.setHourlyRate(BigDecimal.valueOf(50.00));
        item.setQuantity(5);
        item.setUnitPrice(BigDecimal.valueOf(75.00));
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        // Should use hourly rate calculation: 10 * 50 = 500
        assertEquals(BigDecimal.valueOf(500.00), item.getLineTotal());
    }

    @Test
    void calculateItemLineTotal_ShouldUseUnitPrice_WhenHourlyRateNotAvailable() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setHoursWorked(null);
        item.setHourlyRate(null);
        item.setQuantity(5);
        item.setUnitPrice(BigDecimal.valueOf(75.00));
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        // Should use unit price calculation: 5 * 75 = 375
        assertEquals(BigDecimal.valueOf(375.00), item.getLineTotal());
    }

    @Test
    void calculateItemLineTotal_ShouldHandleZeroValues() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setHoursWorked(0);
        item.setHourlyRate(BigDecimal.valueOf(50.00));
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertEquals(0, item.getLineTotal().compareTo(BigDecimal.ZERO));
    }

    @Test
    void calculateItemLineTotal_ShouldHandleZeroUnitPrice() {
        // Given
        InvoiceItem item = new InvoiceItem();
        item.setQuantity(5);
        item.setUnitPrice(BigDecimal.ZERO);
        
        // When
        InvoiceCalculationUtil.calculateItemLineTotal(item);
        
        // Then
        assertEquals(0, item.getLineTotal().compareTo(BigDecimal.ZERO));
    }
}