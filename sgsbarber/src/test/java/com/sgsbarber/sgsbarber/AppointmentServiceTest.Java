package com.sgsbarber.sgsbarber;

import com.sgsbarber.model.AppointmentModel;
import com.sgsbarber.repository.AppointmentRepository;
import com.sgsbarber.repository.BarberScheduleRepository;
import com.sgsbarber.repository.BlockRepository;
import com.sgsbarber.repository.ServiceRepository;
import com.sgsbarber.service.AppointmentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {
    @Test
    void partialStatusUpdatePreservesAppointmentAndPaymentData() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        AppointmentModel current = existingAppointment();
        when(appointments.findById(7L)).thenReturn(Optional.of(current));
        when(appointments.save(any(AppointmentModel.class))).thenAnswer(call -> call.getArgument(0));
        AppointmentService service = new AppointmentService(
                appointments, mock(BlockRepository.class),
                mock(BarberScheduleRepository.class), mock(ServiceRepository.class));

        AppointmentModel update = new AppointmentModel();
        update.setStatus("No-Show");
        AppointmentModel saved = service.update(7L, update);

        assertEquals("No-Show", saved.getStatus());
        assertEquals(12L, saved.getBarberId());
        assertEquals(4L, saved.getServiceId());
        assertEquals(LocalDate.of(2026, 11, 2), saved.getDate());
        assertEquals(LocalTime.of(10, 30), saved.getStartTime());
        assertEquals(new BigDecimal("25.00"), saved.getExtrasTotal());
    }

    @Test
    void partialCheckoutUpdateStoresPaymentAndCompletion() {
        AppointmentRepository appointments = mock(AppointmentRepository.class);
        when(appointments.findById(7L)).thenReturn(Optional.of(existingAppointment()));
        when(appointments.save(any(AppointmentModel.class))).thenAnswer(call -> call.getArgument(0));
        AppointmentService service = new AppointmentService(
                appointments, mock(BlockRepository.class),
                mock(BarberScheduleRepository.class), mock(ServiceRepository.class));
        OffsetDateTime completedAt = OffsetDateTime.parse("2026-11-02T18:00:00-03:00");

        AppointmentModel update = new AppointmentModel();
        update.setStatus("Concluído");
        update.setDiscount(new BigDecimal("5.00"));
        update.setFinalTotal(new BigDecimal("70.00"));
        update.setPaymentMethod("PIX");
        update.setCompletedAt(completedAt);
        AppointmentModel saved = service.update(7L, update);

        assertEquals("Concluído", saved.getStatus());
        assertEquals(new BigDecimal("5.00"), saved.getDiscount());
        assertEquals(new BigDecimal("70.00"), saved.getFinalTotal());
        assertEquals("PIX", saved.getPaymentMethod());
        assertEquals(completedAt, saved.getCompletedAt());
        assertEquals(12L, saved.getBarberId());
        assertEquals(4L, saved.getServiceId());
    }

    private AppointmentModel existingAppointment() {
        AppointmentModel appointment = new AppointmentModel();
        appointment.setId(7L);
        appointment.setBarberId(12L);
        appointment.setServiceId(4L);
        appointment.setDate(LocalDate.of(2026, 11, 2));
        appointment.setStartTime(LocalTime.of(10, 30));
        appointment.setEndTime(LocalTime.of(11, 0));
        appointment.setDurationMinutes(30);
        appointment.setBufferMinutes(10);
        appointment.setStatus("Confirmado");
        appointment.setPrice(new BigDecimal("50.00"));
        appointment.setExtrasTotal(new BigDecimal("25.00"));
        appointment.setDiscount(BigDecimal.ZERO);
        appointment.setFinalTotal(BigDecimal.ZERO);
        return appointment;
    }
}
