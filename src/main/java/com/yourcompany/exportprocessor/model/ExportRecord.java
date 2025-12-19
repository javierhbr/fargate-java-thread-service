package com.yourcompany.exportprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRecord {

    private String id;
    private String type;
    private Map<String, Object> data;
    private Instant timestamp;
}
