package com.rishika.backend.dto;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryDTO {
    private int userId;
    private String firstName;
    private String lastName;
    private String email;
    private String userInfo;
}
