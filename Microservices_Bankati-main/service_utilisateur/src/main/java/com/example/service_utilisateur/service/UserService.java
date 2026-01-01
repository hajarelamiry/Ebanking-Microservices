package com.example.service_utilisateur.service;


import com.example.service_utilisateur.config.UserMapper;
import com.example.service_utilisateur.dto.UserDto;
import com.example.service_utilisateur.model.User;
import com.example.service_utilisateur.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserMapper mapper;

    public UserDto getUserById(Long id){
        return mapper.toDTO(this.userRepository.findById(id).get());
    }
}
