package com.lab.reservation.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchAuditRequest {
    private List<Long> ids;
    private Integer status;
    private String rejectReason;
}
