package pl.matkmiec.backend.weather.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final WeatherProperties weatherProperties;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .enable(
                        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                        DeserializationFeature.ACCEPT_FLOAT_AS_INT,
                        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY
                )
                .disable(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
                )
                .withCoercionConfig(LogicalType.Integer, cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .withCoercionConfig(LogicalType.Float, cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .withCoercionConfig(LogicalType.Collection, cfg ->
                        cfg.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
    }

    @Bean
    public RestClient imgwRestClient(ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(weatherProperties.imgwBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    @Bean
    public RestClient openMeteoRestClient(ObjectMapper objectMapper) {
        return RestClient.builder()
                .baseUrl(weatherProperties.openMeteoBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }
}
