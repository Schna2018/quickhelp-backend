package com.quickhelp.service;

import com.quickhelp.entity.Rol;
import com.quickhelp.repository.RolRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RolService {

    private final RolRepository rolRepository; // Acceso a datos de la tabla rol

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll(); // Obtiene todos los roles
    }
}