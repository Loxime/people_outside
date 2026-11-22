package fr.hesias.dmd.infrastructure.config;

import fr.hesias.dmd.application.CreateReportUseCase;
import fr.hesias.dmd.application.SearchReportsUseCase;
import fr.hesias.dmd.application.port.out.ReportSearchPort;
import fr.hesias.dmd.application.port.out.ReportRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class ReportConfiguration {

    @Bean
    Clock reportClock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateReportUseCase createReportUseCase(
            ReportRepository repository,
            Clock reportClock
    ) {
        return new CreateReportUseCase(repository, reportClock);
    }

    @Bean
    SearchReportsUseCase searchReportsUseCase(
            ReportSearchPort repository,
            Clock reportClock
    ) {
        return new SearchReportsUseCase(repository, reportClock);
    }
}
