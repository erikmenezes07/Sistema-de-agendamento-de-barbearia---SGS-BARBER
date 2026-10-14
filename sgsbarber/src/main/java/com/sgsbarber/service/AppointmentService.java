package com.sgsbarber.service;

import com.sgsbarber.dto.AvailabilityResponse;
import com.sgsbarber.model.AppointmentModel;
import com.sgsbarber.model.BarberScheduleModel;
import com.sgsbarber.model.BlockModel;
import com.sgsbarber.model.ServiceModel;
import com.sgsbarber.repository.AppointmentRepository;
import com.sgsbarber.repository.BarberScheduleRepository;
import com.sgsbarber.repository.BlockRepository;
import com.sgsbarber.repository.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class AppointmentService {
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(19, 0);

    private final AppointmentRepository appointments;
    private final BlockRepository blocks;
    private final BarberScheduleRepository schedules;
    private final ServiceRepository services;

    public AppointmentService(AppointmentRepository appointments, BlockRepository blocks,
                              BarberScheduleRepository schedules, ServiceRepository services) {
        this.appointments = appointments;
        this.blocks = blocks;
        this.schedules = schedules;
        this.services = services;
    }

    public List<AppointmentModel> listAll() { return appointments.findAll(); }
    public AppointmentModel findById(@NonNull Long id) { return appointments.findById(id).orElseThrow(); }

    @Transactional
    public AppointmentModel create(@NonNull AppointmentModel appointment) {
        return saveValidated(appointment, null);
    }

    @Transactional
    public AppointmentModel update(@NonNull Long id, @NonNull AppointmentModel appointment) {
        appointment.setId(id);
        return saveValidated(appointment, id);
    }

    public void delete(@NonNull Long id) { appointments.deleteById(id); }

    public AvailabilityResponse availability(@NonNull Long barberId, @NonNull Long serviceId, @NonNull LocalDate date) {
        ServiceModel service = service(serviceId);
        int duration = service.getDuration() == null ? 30 : service.getDuration();
        Window window = workingWindow(barberId, date);
        List<AvailabilityResponse.Slot> slots = new ArrayList<>();
        if (window.closed()) return new AvailabilityResponse(barberId, serviceId, date, slots);
        for (LocalTime time = window.open(); !time.plusMinutes(duration).plusMinutes(window.buffer()).isAfter(window.close()); time = time.plusMinutes(30)) {
            String reason = conflictReason(barberId, date, time, time.plusMinutes(duration), window.buffer(), null);
            slots.add(new AvailabilityResponse.Slot(time.toString(), reason == null, reason));
        }
        return new AvailabilityResponse(barberId, serviceId, date, slots);
    }

    private AppointmentModel saveValidated(AppointmentModel appointment, Long ignoredId) {
        if (appointment.getBarberId() == null || appointment.getServiceId() == null || appointment.getDate() == null || appointment.getStartTime() == null)
            throw bad("barberId, serviceId, date e time são obrigatórios");
        ServiceModel service = service(Objects.requireNonNull(appointment.getServiceId()));
        int duration = appointment.getDurationMinutes() == null
                ? (service.getDuration() == null ? 30 : service.getDuration())
                : appointment.getDurationMinutes();
        if (duration <= 0) throw bad("A duração do serviço deve ser maior que zero");
        Window window = workingWindow(appointment.getBarberId(), appointment.getDate());
        if (window.closed()) throw bad("O barbeiro não atende nesta data");
        int buffer = appointment.getBufferMinutes() == null ? window.buffer() : appointment.getBufferMinutes();
        LocalTime end = appointment.getStartTime().plusMinutes(duration);
        if (appointment.getStartTime().isBefore(window.open()) || end.plusMinutes(buffer).isAfter(window.close()))
            throw bad("O horário está fora do expediente");
        if (window.breakStart() != null && overlaps(appointment.getStartTime(), end.plusMinutes(buffer), window.breakStart(), window.breakEnd()))
            throw bad("O horário coincide com o intervalo");
        String conflict = conflictReason(appointment.getBarberId(), appointment.getDate(), appointment.getStartTime(), end, buffer, ignoredId);
        if (conflict != null) throw bad(conflict);
        appointment.setDurationMinutes(duration);
        appointment.setEndTime(end);
        if (appointment.getStatus() == null) appointment.setStatus("scheduled");
        return appointments.save(appointment);
    }

    private String conflictReason(Long barberId, LocalDate date, LocalTime start, LocalTime end, int buffer, Long ignoredId) {
        for (AppointmentModel other : appointments.findByBarberIdAndDate(barberId, date)) {
            if (ignoredId != null && ignoredId.equals(other.getId())) continue;
            if (other.getStartTime() == null || other.getEndTime() == null || isCancelled(other.getStatus())) continue;
            if (overlaps(start, end.plusMinutes(buffer), other.getStartTime(), other.getEndTime().plusMinutes(other.getBufferMinutes() == null ? 0 : other.getBufferMinutes()))) return "Horário já ocupado";
        }
        for (BlockModel block : blocks.findByDate(date)) {
            if (block.getBarberId() == null || block.getBarberId().equals(barberId)) {
                if (overlaps(start, end.plusMinutes(buffer), block.getStartTime(), block.getEndTime())) return "Horário bloqueado";
            }
        }
        return null;
    }

    private Window workingWindow(Long barberId, LocalDate date) {
        int weekday = date.getDayOfWeek().getValue() % 7;
        List<BarberScheduleModel> rows = schedules.findByBarberIdAndWeekday(barberId, (short) weekday);
        if (rows.isEmpty()) return new Window(DEFAULT_OPEN, DEFAULT_CLOSE, null, null, 0);
        BarberScheduleModel row = rows.get(0);
        if (!"working".equalsIgnoreCase(row.getType()) || row.getOpenTime() == null || row.getCloseTime() == null)
            return Window.closedWindow();
        return new Window(row.getOpenTime(), row.getCloseTime(), row.getBreakStart(), row.getBreakEnd(), row.getBufferMinutes() == null ? 0 : row.getBufferMinutes());
    }

    private ServiceModel service(@NonNull Long id) { return services.findById(id).orElseThrow(() -> bad("Serviço não encontrado")); }
    private boolean isCancelled(String status) { return status != null && (status.equalsIgnoreCase("cancelled") || status.equalsIgnoreCase("cancelado") || status.equalsIgnoreCase("no-show")); }
    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) { return bStart != null && bEnd != null && aStart.isBefore(bEnd) && aEnd.isAfter(bStart); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private record Window(LocalTime open, LocalTime close, LocalTime breakStart, LocalTime breakEnd, int buffer) {
        static Window closedWindow() { return new Window(null, null, null, null, 0); }
        boolean closed() { return open == null || close == null; }
    }
}
