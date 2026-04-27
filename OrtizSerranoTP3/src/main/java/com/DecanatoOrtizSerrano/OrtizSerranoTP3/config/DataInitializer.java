package com.DecanatoOrtizSerrano.OrtizSerranoTP3.config;

import com.DecanatoOrtizSerrano.OrtizSerranoTP3.model.Administrador;
import com.DecanatoOrtizSerrano.OrtizSerranoTP3.repository.AdministradorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Inicializador de datos: crea el primer administrador si no existe ninguno.
 *
 * Credenciales por defecto:
 *   Email:    admin@decanato.edu
 *   Password: Admin1234
 *
 * ⚠️  Cambiar la contraseña luego del primer inicio de sesión.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private AdministradorRepository administradorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (administradorRepository.count() == 0) {
            Administrador admin = new Administrador(
                    "Admin",
                    "Sistema",
                    "admin@decanato.edu",
                    passwordEncoder.encode("Admin1234"),
                    "ADMINISTRADOR",
                    "Decanato",
                    1
            );
            administradorRepository.save(admin);
            log.info("==========================================================");
            log.info("  ✅ Usuario administrador inicial creado:");
            log.info("     Email:    admin@decanato.edu");
            log.info("     Password: Admin1234");
            log.info("  ⚠️  Cambiá la contraseña después del primer login.");
            log.info("==========================================================");
        }
    }
}
