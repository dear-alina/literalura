package com.alurachallenge.literalura;

import com.alurachallenge.literalura.repository.AutorRepository;
import com.alurachallenge.literalura.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest(properties = {
        "app.interactive=false"
})
class LiteraluraApplicationTests {

    @MockitoBean
    private LibroRepository libroRepository;

    @MockitoBean
    private AutorRepository autorRepository;

    @Autowired
    private LiteraluraApplication application;

    @Test
    void contextLoads() {
        assertThat(application).isNotNull();
    }

    @Test
    void main_deberiaDelegarEnSpringApplicationRun() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(LiteraluraApplication.class, new String[]{}))
                    .thenReturn(context);

            LiteraluraApplication.main(new String[]{});

            springApplication.verify(() -> SpringApplication.run(LiteraluraApplication.class, new String[]{}));
        }
    }
}

