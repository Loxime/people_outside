package fr.hesias.dmd.web;

import fr.hesias.dmd.application.CreateReportUseCase;
import fr.hesias.dmd.application.SearchReportsUseCase;
import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateReportUseCase useCase;

    @MockitoBean
    private SearchReportsUseCase searchUseCase;

    @Test
    void shouldCreateReport() throws Exception {
        Instant timestamp =
                Instant.parse("2026-11-22T16:30:00Z");

        Report saved = new Report(
                42L, 48.8566, 2.3522,
                PlaceType.PARK, 2, timestamp
        );

        when(useCase.create(
                48.8566, 2.3522, PlaceType.PARK, 2
        )).thenReturn(saved);

        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 48.8566,
                                  "longitude": 2.3522,
                                  "placeType": "PARK",
                                  "crowdLevel": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.latitude").value(48.8566))
                .andExpect(jsonPath("$.longitude").value(2.3522))
                .andExpect(jsonPath("$.placeType").value("PARK"))
                .andExpect(jsonPath("$.crowdLevel").value(2))
                .andExpect(jsonPath("$.timestamp")
                        .value("2026-11-22T16:30:00Z"));

        verify(useCase).create(
                48.8566, 2.3522, PlaceType.PARK, 2
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {
              "longitude": 2.3522,
              "placeType": "PARK",
              "crowdLevel": 2
            }
            """,
            """
            {
              "latitude": 91,
              "longitude": 2.3522,
              "placeType": "PARK",
              "crowdLevel": 2
            }
            """,
            """
            {
              "latitude": 48.8566,
              "longitude": -181,
              "placeType": "PARK",
              "crowdLevel": 2
            }
            """,
            """
            {
              "latitude": 48.8566,
              "longitude": 2.3522,
              "placeType": null,
              "crowdLevel": 2
            }
            """,
            """
            {
              "latitude": 48.8566,
              "longitude": 2.3522,
              "placeType": "PARK",
              "crowdLevel": 0
            }
            """,
            """
            {
              "latitude": 48.8566,
              "longitude": 2.3522,
              "placeType": "PARK",
              "crowdLevel": 6
            }
            """,
            """
            {
              "latitude": 48.8566,
              "longitude": 2.3522,
              "placeType": "UNKNOWN",
              "crowdLevel": 2
            }
            """
    })
    void shouldRejectInvalidRequest(String body) throws Exception {
        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @Test
    void shouldFindNearbyReports() throws Exception {
        Report first = new Report(
                21L, 48.8566, 2.3522,
                PlaceType.PARK, 2,
                Instant.parse("2026-11-22T17:16:46Z")
        );

        Report second = new Report(
                22L, 48.8580, 2.3500,
                PlaceType.PLAYGROUND, 4,
                Instant.parse("2026-11-22T17:20:00Z")
        );

        when(searchUseCase.search(48.8566, 2.3522, 10))
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/reports")
                        .param("latitude", "48.8566")
                        .param("longitude", "2.3522")
                        .param("radiusKm", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].placeType").value("PARK"))
                .andExpect(jsonPath("$[0].crowdLevel").value(2))
                .andExpect(jsonPath("$[0].timestamp")
                        .value("2026-11-22T17:16:46Z"))
                .andExpect(jsonPath("$[1].id").value(22))
                .andExpect(jsonPath("$[1].placeType")
                        .value("PLAYGROUND"));

        verify(searchUseCase).search(48.8566, 2.3522, 10);
        verifyNoInteractions(useCase);
    }

    @Test
    void shouldReturnEmptyArrayWhenNoReportsMatch() throws Exception {
        when(searchUseCase.search(48.8566, 2.3522, 1))
                .thenReturn(List.of());

        mockMvc.perform(get("/reports")
                        .param("latitude", "48.8566")
                        .param("longitude", "2.3522")
                        .param("radiusKm", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(searchUseCase).search(48.8566, 2.3522, 1);
    }

    @Test
    void shouldAcceptMaximumRadius() throws Exception {
        when(searchUseCase.search(48.8566, 2.3522, 100))
                .thenReturn(List.of());

        mockMvc.perform(get("/reports")
                        .param("latitude", "48.8566")
                        .param("longitude", "2.3522")
                        .param("radiusKm", "100"))
                .andExpect(status().isOk());

        verify(searchUseCase).search(48.8566, 2.3522, 100);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "longitude=2.3522&radiusKm=10",
            "latitude=48.8566&radiusKm=10",
            "latitude=48.8566&longitude=2.3522",
            "latitude=abc&longitude=2.3522&radiusKm=10",
            "latitude=48.8566&longitude=nope&radiusKm=10",
            "latitude=48.8566&longitude=2.3522&radiusKm=abc"
    })
    void shouldRejectMissingOrMalformedParameters(
            String query
    ) throws Exception {
        mockMvc.perform(get("/reports?" + query))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(searchUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "latitude=91&longitude=2.3522&radiusKm=10",
            "latitude=-91&longitude=2.3522&radiusKm=10",
            "latitude=48.8566&longitude=181&radiusKm=10",
            "latitude=48.8566&longitude=-181&radiusKm=10",
            "latitude=48.8566&longitude=2.3522&radiusKm=0",
            "latitude=48.8566&longitude=2.3522&radiusKm=100.01",
            "latitude=48.8566&longitude=2.3522&radiusKm=NaN",
            "latitude=48.8566&longitude=2.3522&radiusKm=Infinity"
    })
    void shouldReturnBadRequestForDomainValidation(
            String query
    ) throws Exception {
        when(searchUseCase.search(
                anyDouble(), anyDouble(), anyDouble()
        )).thenThrow(new IllegalArgumentException("Invalid search"));

        mockMvc.perform(get("/reports?" + query))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            """
            {"latitude":48.8566,"longitude":2.3522,
             "placeType":"PARK","crowdLevel":2.5}
            """,
            """
            {"latitude":48.8566,"longitude":2.3522,
             "placeType":"PARK","crowdLevel":"2"}
            """,
            """
            {"latitude":48.8566,"longitude":2.3522,
             "placeType":0,"crowdLevel":2}
            """,
            """
            {"latitude":"48.8566","longitude":2.3522,
             "placeType":"PARK","crowdLevel":2}
            """,
            """
            {"latitude":48.8566,"longitude":"2.3522",
             "placeType":"PARK","crowdLevel":2}
            """,
            """
            {"latitude":48.8566,"longitude":2.3522,
             "placeType":"PARK","crowdLevel":true}
            """,
            """
            {"latitude":48.8566,"longitude":2.3522,
             "placeType":"PARK","crowdLevel":2,"id":999}
            """,
            """
            {"latitude":48.8566,"longitude":2.3522,
             "placeType":"PARK","crowdLevel":2,
             "timestamp":"2026-11-22T12:00:00Z"}
            """,
            """
            {"latitude":48.8566,"longitude":2.3522,
             "placeType":"PARK","crowdLevel":2,"extra":"value"}
            """
    })
    void shouldRejectCoercedJsonValues(String body) throws Exception {
        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(useCase, searchUseCase);
    }

    @Test
    void shouldAcceptIntegerCoordinatesAsJsonNumbers()
            throws Exception {
        Report saved = new Report(
                43L, 0, 0, PlaceType.PARK, 1,
                Instant.parse("2026-11-22T12:00:00Z")
        );

        when(useCase.create(0, 0, PlaceType.PARK, 1))
                .thenReturn(saved);

        mockMvc.perform(post("/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 0,
                                  "longitude": 0,
                                  "placeType": "PARK",
                                  "crowdLevel": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(43));

        verify(useCase).create(0, 0, PlaceType.PARK, 1);
    }
}
