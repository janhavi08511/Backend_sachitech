package com.example.sachitech.dto;

import com.example.sachitech.entity.Role;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;
}
