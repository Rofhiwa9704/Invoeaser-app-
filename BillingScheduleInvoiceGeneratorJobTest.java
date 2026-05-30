package co.za.kingstechco.kingstechco.invoeaserapp.job;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.BillingSchedule;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.Invoice;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.BillingScheduleRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.InvoiceRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingScheduleInvoiceGeneratorJobTest {

    @Mock
    private BillingScheduleRepository billingScheduleRepository;
    
    @Mock
    private InvoiceService invoiceService;
    
    @Mock
    private InvoiceRepository invoiceRepository;

    private BillingScheduleInvoiceGeneratorJob billingScheduleJob;

    private static final Long TENANT_ID = 1L;
    private static final Long SCHEDULE_ID = 100L;
    private static final LocalDate TEST_DATE = LocalDate.of(2024, 3, 15); // Friday, March 15, 2024

    @BeforeEach
    void setUp() {
        billingScheduleJob = new BillingScheduleInvoiceGeneratorJob(
                billingScheduleRepository, invoiceService, invoiceRepository);
    }

    @Test
    void generateInvoicesFromSchedules_ShouldProcessActiveSchedules_WhenSchedulesExist() {
        // Given
        List<BillingSchedule> schedules = Arrays.asList(
                createDailySchedule(),
                createWeeklySchedule(),
                createMonthlySchedule()
        );
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(any(), any())).thenReturn(false);
        
        // Mock the invoice service to return valid invoices
        Invoice mockInvoice = new Invoice();
        mockInvoice.setInvoiceId(1L);
        mockInvoice.setInvoiceReference("INV-001");
        when(invoiceService.generateFromSchedule(any(), any())).thenReturn(mockInvoice);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(billingScheduleRepository).findAllByActiveTrue();
            verify(invoiceService, times(1)).generateFromSchedule(any(), eq(TEST_DATE)); // Only daily should generate
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldSkipInvoiceGeneration_WhenInvoiceAlreadyExists() {
        // Given
        BillingSchedule dailySchedule = createDailySchedule();
        List<BillingSchedule> schedules = Collections.singletonList(dailySchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(TENANT_ID, TEST_DATE)).thenReturn(true);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(billingScheduleRepository).findAllByActiveTrue();
            verify(invoiceRepository).existsByTenantIdAndInvoiceDate(TENANT_ID, TEST_DATE);
            verify(invoiceService, never()).generateFromSchedule(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldHandleExceptions_GracefullyAndContinue() {
        // Given
        List<BillingSchedule> schedules = Arrays.asList(
                createDailySchedule(),
                createAnotherDailySchedule()
        );
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(any(), any())).thenReturn(false);
        
        // The first schedule throws exception, second should still be processed
        when(invoiceService.generateFromSchedule(eq(schedules.get(0)), eq(TEST_DATE)))
                .thenThrow(new RuntimeException("Invoice generation failed"));
        when(invoiceService.generateFromSchedule(eq(schedules.get(1)), eq(TEST_DATE)))
                .thenReturn(new Invoice()); // Return a mock invoice

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService, times(2)).generateFromSchedule(any(), eq(TEST_DATE));
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldNotProcessSchedules_WhenNoActiveSchedules() {
        // Given
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(Collections.emptyList());

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(billingScheduleRepository).findAllByActiveTrue();
            verify(invoiceService, never()).generateFromSchedule(any(), any());
            verify(invoiceRepository, never()).existsByTenantIdAndInvoiceDate(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldGenerateDailyInvoices_OnAnyDay() {
        // Given
        BillingSchedule dailySchedule = createDailySchedule();
        List<BillingSchedule> schedules = Collections.singletonList(dailySchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(any(), any())).thenReturn(false);
        
        // Mock the invoice service to return valid invoices
        Invoice mockInvoice = new Invoice();
        mockInvoice.setInvoiceId(1L);
        mockInvoice.setInvoiceReference("INV-001");
        when(invoiceService.generateFromSchedule(any(), any())).thenReturn(mockInvoice);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService).generateFromSchedule(dailySchedule, TEST_DATE);
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldGenerateWeeklyInvoices_OnCorrectDayOfWeek() {
        // Given
        BillingSchedule weeklySchedule = createWeeklySchedule();
        weeklySchedule.setStartDate(TEST_DATE); // Start date is Friday (same as test date)
        List<BillingSchedule> schedules = Collections.singletonList(weeklySchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(any(), any())).thenReturn(false);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService).generateFromSchedule(weeklySchedule, TEST_DATE);
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldNotGenerateWeeklyInvoices_OnWrongDayOfWeek() {
        // Given
        BillingSchedule weeklySchedule = createWeeklySchedule();
        weeklySchedule.setStartDate(LocalDate.of(2024, 3, 11)); // Monday, different from Friday test date
        List<BillingSchedule> schedules = Collections.singletonList(weeklySchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE); // Friday

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService, never()).generateFromSchedule(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldGenerateMonthlyInvoices_OnCorrectDayOfMonth() {
        // Given
        BillingSchedule monthlySchedule = createMonthlySchedule();
        monthlySchedule.setDayOfMonth(15); // Same as test date day (15th)
        List<BillingSchedule> schedules = Collections.singletonList(monthlySchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(any(), any())).thenReturn(false);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService).generateFromSchedule(monthlySchedule, TEST_DATE);
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldNotGenerateMonthlyInvoices_OnWrongDayOfMonth() {
        // Given
        BillingSchedule monthlySchedule = createMonthlySchedule();
        monthlySchedule.setDayOfMonth(20); // Different from test date day (15th)
        List<BillingSchedule> schedules = Collections.singletonList(monthlySchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService, never()).generateFromSchedule(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldHandleUnknownFrequency_Gracefully() {
        // Given
        BillingSchedule unknownSchedule = createDailySchedule();
        unknownSchedule.setFrequency("UNKNOWN");
        List<BillingSchedule> schedules = Collections.singletonList(unknownSchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService, never()).generateFromSchedule(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldThrowException_WhenRepositoryFails() {
        // Given
        when(billingScheduleRepository.findAllByActiveTrue())
                .thenThrow(new RuntimeException("Database connection error"));

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> billingScheduleJob.generateInvoicesFromSchedules());
            
            assertEquals("Automated invoice generation job failed", exception.getMessage());
            verify(billingScheduleRepository).findAllByActiveTrue();
            verify(invoiceService, never()).generateFromSchedule(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldSkipSchedule_WhenNotYetActive() {
        // Given
        BillingSchedule futureSchedule = createDailySchedule();
        futureSchedule.setStartDate(TEST_DATE.plusDays(1)); // Starts tomorrow
        List<BillingSchedule> schedules = Collections.singletonList(futureSchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService, never()).generateFromSchedule(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldSkipSchedule_WhenExpired() {
        // Given
        BillingSchedule expiredSchedule = createDailySchedule();
        expiredSchedule.setEndDate(TEST_DATE.minusDays(1)); // Ended yesterday
        List<BillingSchedule> schedules = Collections.singletonList(expiredSchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService, never()).generateFromSchedule(any(), any());
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldProcessMultipleTenants_Independently() {
        // Given
        BillingSchedule tenant1Schedule = createDailySchedule();
        tenant1Schedule.setTenantId(1L);
        
        BillingSchedule tenant2Schedule = createDailySchedule();
        tenant2Schedule.setTenantId(2L);
        tenant2Schedule.setId(200L);
        
        List<BillingSchedule> schedules = Arrays.asList(tenant1Schedule, tenant2Schedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(1L, TEST_DATE)).thenReturn(false);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(2L, TEST_DATE)).thenReturn(false);
        
        // Mock invoices for both tenants
        Invoice mockInvoice1 = new Invoice();
        mockInvoice1.setInvoiceId(1L);
        mockInvoice1.setInvoiceReference("INV-001");
        when(invoiceService.generateFromSchedule(tenant1Schedule, TEST_DATE)).thenReturn(mockInvoice1);
        
        Invoice mockInvoice2 = new Invoice();
        mockInvoice2.setInvoiceId(2L);
        mockInvoice2.setInvoiceReference("INV-002");
        when(invoiceService.generateFromSchedule(tenant2Schedule, TEST_DATE)).thenReturn(mockInvoice2);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then
            verify(invoiceService).generateFromSchedule(tenant1Schedule, TEST_DATE);
            verify(invoiceService).generateFromSchedule(tenant2Schedule, TEST_DATE);
            verify(invoiceRepository).existsByTenantIdAndInvoiceDate(1L, TEST_DATE);
            verify(invoiceRepository).existsByTenantIdAndInvoiceDate(2L, TEST_DATE);
        }
    }

    @Test
    void generateInvoicesFromSchedules_ShouldCheckForDuplicates_BeforeGeneration() {
        // Given
        BillingSchedule dailySchedule = createDailySchedule();
        List<BillingSchedule> schedules = Collections.singletonList(dailySchedule);
        
        when(billingScheduleRepository.findAllByActiveTrue()).thenReturn(schedules);
        when(invoiceRepository.existsByTenantIdAndInvoiceDate(TENANT_ID, TEST_DATE)).thenReturn(false);
        
        // Mock invoice for the duplicate test
        Invoice mockInvoice = new Invoice();
        mockInvoice.setInvoiceId(1L);
        mockInvoice.setInvoiceReference("INV-001");
        when(invoiceService.generateFromSchedule(dailySchedule, TEST_DATE)).thenReturn(mockInvoice);

        try (MockedStatic<LocalDate> mockedLocalDate = Mockito.mockStatic(LocalDate.class)) {
            mockedLocalDate.when(LocalDate::now).thenReturn(TEST_DATE);

            // When
            billingScheduleJob.generateInvoicesFromSchedules();

            // Then - Verify order of operations
            var inOrder = inOrder(invoiceRepository, invoiceService);
            inOrder.verify(invoiceRepository).existsByTenantIdAndInvoiceDate(TENANT_ID, TEST_DATE);
            inOrder.verify(invoiceService).generateFromSchedule(dailySchedule, TEST_DATE);
        }
    }

    // Helper methods
    private BillingSchedule createDailySchedule() {
        BillingSchedule schedule = new BillingSchedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setTenantId(TENANT_ID);
        schedule.setFrequency("DAILY");
        schedule.setRate(BigDecimal.valueOf(100.00));
        schedule.setActive(true);
        schedule.setStartDate(LocalDate.of(2024, 1, 1));
        return schedule;
    }

    private BillingSchedule createWeeklySchedule() {
        BillingSchedule schedule = new BillingSchedule();
        schedule.setId(SCHEDULE_ID + 1);
        schedule.setTenantId(TENANT_ID);
        schedule.setFrequency("WEEKLY");
        schedule.setRate(BigDecimal.valueOf(500.00));
        schedule.setActive(true);
        schedule.setStartDate(LocalDate.of(2024, 1, 1)); // Monday
        return schedule;
    }

    private BillingSchedule createMonthlySchedule() {
        BillingSchedule schedule = new BillingSchedule();
        schedule.setId(SCHEDULE_ID + 2);
        schedule.setTenantId(TENANT_ID);
        schedule.setFrequency("MONTHLY");
        schedule.setDayOfMonth(1); // First of month
        schedule.setRate(BigDecimal.valueOf(2000.00));
        schedule.setActive(true);
        schedule.setStartDate(LocalDate.of(2024, 1, 1));
        return schedule;
    }

    private BillingSchedule createAnotherDailySchedule() {
        BillingSchedule schedule = new BillingSchedule();
        schedule.setId(SCHEDULE_ID + 3);
        schedule.setTenantId(TENANT_ID + 1);
        schedule.setFrequency("DAILY");
        schedule.setRate(BigDecimal.valueOf(150.00));
        schedule.setActive(true);
        schedule.setStartDate(LocalDate.of(2024, 1, 1));
        return schedule;
    }
}