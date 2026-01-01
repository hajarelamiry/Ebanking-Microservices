package com.example.service_utilisateur.config;

import com.example.service_utilisateur.dto.UserDto;
import com.example.service_utilisateur.model.User;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@NoArgsConstructor
@AllArgsConstructor
public class UserMapper {
    @Autowired
    private ModelMapper mapper;

    public UserDto toDTO(User user) {
        return mapper.map(user, UserDto.class);
    }

    // Convertir un ExpenseDTO en entité Expense
    public User toEntity(UserDto userDto) {
        return mapper.map(userDto, User.class);
    }
}
