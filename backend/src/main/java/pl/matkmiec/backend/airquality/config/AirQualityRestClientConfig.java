package pl.matkmiec.backend.airquality.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AirQualityRestClientConfig {

    private final AirQualityProperties airQualityProperties;

    @Bean
    public RestClient giosRestClient(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        List<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        mediaTypes.add(MediaType.valueOf("application/ld+json"));
        mediaTypes.add(MediaType.valueOf("application/ld+json;charset=utf-8"));
        mediaTypes.add(MediaType.ALL);
        converter.setSupportedMediaTypes(mediaTypes);

        return RestClient.builder()
                .baseUrl(airQualityProperties.giosBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE, "application/ld+json", "*/*")
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(converter);
                })
                .build();
    }

    @Bean
    public RestClient iosRestClient(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        List<MediaType> mediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        mediaTypes.add(MediaType.ALL);
        converter.setSupportedMediaTypes(mediaTypes);

        return RestClient.builder()
                .baseUrl(airQualityProperties.iosBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE, "*/*")
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(converter);
                })
                .build();
    }
}
