package fr.hesias.dmd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import fr.hesias.dmd.infrastructure.persistence.oracle.SpringDataReportRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
class DmdApplicationTests {

    @MockitoBean
    private SpringDataReportRepository repository;

    @Test
    void contextLoads() {
    }
}
