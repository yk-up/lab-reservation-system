package com.lab.reservation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.lab.reservation.mapper")
@EnableScheduling
public class LabReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabReservationApplication.class, args);
    }
}
