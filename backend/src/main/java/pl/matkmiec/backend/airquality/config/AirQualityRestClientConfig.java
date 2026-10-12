package pl.matkmiec.backend.airquality.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class AirQualityRestClientConfig {

    private final AirQualityProperties airQualityProperties;

    @Bean
    public RestClient giosRestClient(ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(airQualityProperties.giosBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE, "application/ld+json", "*/*")
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    @Bean
    public RestClient iosRestClient(ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(airQualityProperties.iosBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE, "*/*")
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }
}
