package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.service.IPetService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Transactional
public class PetService implements IPetService {

}
