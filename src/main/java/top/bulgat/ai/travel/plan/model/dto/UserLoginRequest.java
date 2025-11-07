package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String username;
    private String password;
}