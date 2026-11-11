package com.banco.hr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ManagementServiceApplicationTests {

    @Test
    @DisplayName("Deve iniciar o contexto da aplicação com o perfil de teste")
    void contextLoads() {
        // Verificação: a inicialização do contexto pelo Spring é o resultado esperado deste teste.
    }
}