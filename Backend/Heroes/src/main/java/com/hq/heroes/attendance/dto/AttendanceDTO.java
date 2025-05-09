package com.hq.heroes.attendance.dto;

import com.hq.heroes.attendance.entity.enums.AttendanceStatus;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {

    private Long attendanceId;
    private String employeeId;
    private String employeeName;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private AttendanceStatus status;


    // 근무 시간 계산 메서드
    public double getWorkHours() {
        if (checkIn != null && checkOut != null) {
            Duration duration = Duration.between(checkIn, checkOut);

            return duration.toMinutes() / 60.0;
        }
        return 0;
    }
}
