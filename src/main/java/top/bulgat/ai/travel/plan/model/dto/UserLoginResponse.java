package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

@Data
public class UserLoginResponse {
    private String token;
    private String message;
}