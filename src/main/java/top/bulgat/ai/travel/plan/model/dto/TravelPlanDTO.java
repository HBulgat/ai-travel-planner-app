package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TravelPlanDTO {
    private String id;
    private String userId;
    private String planName;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> activities;
    private String notes;
}