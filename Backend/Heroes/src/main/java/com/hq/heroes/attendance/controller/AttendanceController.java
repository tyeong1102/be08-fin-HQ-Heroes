package com.hq.heroes.attendance.controller;

import com.hq.heroes.attendance.dto.AttendanceDTO;
import com.hq.heroes.attendance.entity.Attendance;
import com.hq.heroes.attendance.service.AttendanceService;
import com.hq.heroes.auth.dto.form.CustomEmployeeDetails;
import com.hq.heroes.auth.entity.Employee;
import com.hq.heroes.auth.repository.EmployeeRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/my-attendance")
    @Operation(summary = "로그인한 사용자의 근태 기록 조회")
    public ResponseEntity<List<AttendanceDTO>> getMyAttendanceRecords(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            CustomEmployeeDetails userDetails = (CustomEmployeeDetails) authentication.getPrincipal();
            String employeeId = userDetails.getUsername();

            List<AttendanceDTO> records = attendanceService.getAttendancesByEmployeeId(employeeId);
            return ResponseEntity.ok(records);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/total-work-hours")
    @Operation(summary = "로그인한 사용자의 월별 총 근무 시간 조회")
    public ResponseEntity<Integer> getTotalWorkHours(
            Authentication authentication,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        if (authentication != null && authentication.isAuthenticated()) {
            CustomEmployeeDetails userDetails = (CustomEmployeeDetails) authentication.getPrincipal();
            String employeeId = userDetails.getUsername();

            YearMonth targetMonth = YearMonth.of(year, month).minusMonths(1);

            int totalWorkHours = attendanceService.calculateTotalWorkHours(employeeId, targetMonth);

            return ResponseEntity.ok(totalWorkHours);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/my-work-time")
    @Operation(summary = "로그인한 사용자의 근무 시간 조회")
    public ResponseEntity<Map<String, Double>> getMyWorkTime(Authentication authentication, @RequestParam YearMonth yearMonth) {
        if (authentication != null && authentication.isAuthenticated()) {
            CustomEmployeeDetails userDetails = (CustomEmployeeDetails) authentication.getPrincipal();
            String employeeId = userDetails.getUsername();

            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            List<AttendanceDTO> records = attendanceService.findByEmployee_IdAndDateBetween(employeeId, startDate, endDate);

            double totalWorkHours = records.stream()
                    .mapToDouble(AttendanceDTO::getWorkHours)
                    .sum();

            return ResponseEntity.ok(Map.of("totalWorkHours", totalWorkHours));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/check-in")
    @Operation(summary = "출근 요청")
    public ResponseEntity<?> checkIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomEmployeeDetails) {
                CustomEmployeeDetails userDetails = (CustomEmployeeDetails) principal;
                String employeeId = userDetails.getUsername();

                Optional<Employee> employeeOptional = employeeRepository.findByEmployeeId(employeeId);
                if (employeeOptional.isPresent()) {
                    Attendance attendance = attendanceService.checkIn(employeeOptional.get());
                    return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("attendanceId", attendance.getAttendanceId()));
                }
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/check-out")
    @Operation(summary = "퇴근 요청")
    public ResponseEntity<?> checkOut() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomEmployeeDetails) {
                CustomEmployeeDetails userDetails = (CustomEmployeeDetails) principal;
                String employeeId = userDetails.getUsername();

                Optional<Employee> employeeOptional = employeeRepository.findByEmployeeId(employeeId);
                if (employeeOptional.isPresent()) {
                    attendanceService.checkOut(employeeOptional.get());
                    return ResponseEntity.ok().build();
                }
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/status")
    @Operation(summary = "출근 상태 확인 요청")
    public ResponseEntity<Map<String, Boolean>> checkStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            CustomEmployeeDetails userDetails = (CustomEmployeeDetails) authentication.getPrincipal();
            String employeeId = userDetails.getUsername();

            boolean isCheckedIn = attendanceService.isAlreadyCheckedIn(employeeId);
            return ResponseEntity.ok(Map.of("isCheckedIn", isCheckedIn));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/latest/{employeeId}")
    @Operation(summary = "사용자 ID를 사용하여 최신 출석 기록 조회")
    public ResponseEntity<AttendanceDTO> getLatestAttendance(@PathVariable String employeeId) {
        AttendanceDTO attendanceDTO = attendanceService.getLatestAttendance(employeeId);
        return ResponseEntity.ok(attendanceDTO);
    }

    @GetMapping("/latest-record/{employeeId}")
    @Operation(summary = "사용자 ID를 사용하여 최신 출석 기록 조회")
    public ResponseEntity<AttendanceDTO> getLatestAttendanceRecord(@PathVariable String employeeId) {
        AttendanceDTO latestAttendance = attendanceService.getLatestAttendanceRecord(employeeId);

        if (latestAttendance != null) {
            return ResponseEntity.ok(latestAttendance);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

}