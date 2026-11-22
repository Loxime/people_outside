package fr.hesias.dmd.web;

import com.jayway.jsonpath.JsonPath;
import fr.hesias.dmd.application.port.out.ReportRepository;
import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;
import fr.hesias.dmd.infrastructure.persistence.oracle.SpringDataReportRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ReportHttpOracleIT {

    private static final Instant NOW =
            Instant.parse("2035-11-22T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportRepository repository;

    @Autowired
    private SpringDataReportRepository springDataRepository;

    @Autowired
    private EntityManager entityManager;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Test
    void shouldCreatePersistAndFindReportThroughHttp() throws Exception {
        String createdJson = mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 12.3456,
                                  "longitude": 34.5678,
                                  "placeType": "PARK",
                                  "crowdLevel": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placeType").value("PARK"))
                .andExpect(jsonPath("$.crowdLevel").value(3))
                .andExpect(jsonPath("$.timestamp").value(NOW.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = ((Number) JsonPath.read(createdJson, "$.id"))
                .longValue();

        entityManager.flush();
        entityManager.clear();

        assertTrue(springDataRepository.existsById(id));

        List<Long> ids = searchIds(12.3456, 34.5678, 1);

        assertTrue(ids.contains(id));
    }

    @Test
    void shouldFilterDateAndDistanceThroughHttp() throws Exception {
        Report nearbyToday = save(
                12.3456, 34.5678, NOW
        );

        Report distantToday = save(
                15.0, 34.5678, NOW
        );

        Report nearbyYesterday = save(
                12.3456, 34.5678,
                Instant.parse("2035-11-21T12:00:00Z")
        );

        entityManager.flush();
        entityManager.clear();

        List<Long> ids = searchIds(12.3456, 34.5678, 10);

        assertAll(
                () -> assertTrue(ids.contains(nearbyToday.id())),
                () -> assertFalse(ids.contains(distantToday.id())),
                () -> assertFalse(ids.contains(nearbyYesterday.id()))
        );
    }

    @Test
    void shouldRejectInvalidHttpRequestWithoutPersisting()
            throws Exception {
        long before = springDataRepository.count();

        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 91,
                                  "longitude": 34.5678,
                                  "placeType": "PARK",
                                  "crowdLevel": 3
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(before, springDataRepository.count());
    }

    private Report save(
            double latitude,
            double longitude,
            Instant timestamp
    ) {
        return repository.save(new Report(
                null,
                latitude,
                longitude,
                PlaceType.PARK,
                3,
                timestamp
        ));
    }

    private List<Long> searchIds(
            double latitude,
            double longitude,
            double radiusKm
    ) throws Exception {
        String json = mockMvc.perform(get("/reports")
                        .param("latitude", Double.toString(latitude))
                        .param("longitude", Double.toString(longitude))
                        .param("radiusKm", Double.toString(radiusKm)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<Number> ids = JsonPath.read(json, "$[*].id");

        return ids.stream()
                .map(Number::longValue)
                .toList();
    }
}
