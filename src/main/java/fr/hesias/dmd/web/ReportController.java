package fr.hesias.dmd.web;

import fr.hesias.dmd.application.CreateReportUseCase;
import fr.hesias.dmd.application.SearchReportsUseCase;
import fr.hesias.dmd.domain.model.PlaceType;
import fr.hesias.dmd.domain.model.Report;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final CreateReportUseCase createReportUseCase;
    private final SearchReportsUseCase searchReportsUseCase;

    public ReportController(
            CreateReportUseCase createReportUseCase,
            SearchReportsUseCase searchReportsUseCase
    ) {
        this.createReportUseCase = createReportUseCase;
        this.searchReportsUseCase = searchReportsUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(
            @Valid @RequestBody CreateReportRequest request
    ) {
        Report created = createReportUseCase.create(
                request.latitude(),
                request.longitude(),
                request.placeType(),
                request.crowdLevel()
        );

        return ReportResponse.from(created);
    }

    @GetMapping
    public List<ReportResponse> search(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double radiusKm
    ) {
        try {
            return searchReportsUseCase
                    .search(latitude, longitude, radiusKm)
                    .stream()
                    .map(ReportResponse::from)
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage(),
                    exception
            );
        }
    }

    public record CreateReportRequest(
            @NotNull
            @DecimalMin("-90")
            @DecimalMax("90")
            Double latitude,

            @NotNull
            @DecimalMin("-180")
            @DecimalMax("180")
            Double longitude,

            @NotNull
            PlaceType placeType,

            @NotNull
            @Min(1)
            @Max(5)
            Integer crowdLevel
    ) {
    }

    public record ReportResponse(
            Long id,
            double latitude,
            double longitude,
            PlaceType placeType,
            int crowdLevel,
            Instant timestamp
    ) {
        static ReportResponse from(Report report) {
            return new ReportResponse(
                    report.id(),
                    report.latitude(),
                    report.longitude(),
                    report.placeType(),
                    report.crowdLevel(),
                    report.timestamp()
            );
        }
    }
}
