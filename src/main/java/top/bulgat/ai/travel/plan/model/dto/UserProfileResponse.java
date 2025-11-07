package top.bulgat.ai.travel.plan.model.dto;

import lombok.Data;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    // Add other user profile fields as needed
}