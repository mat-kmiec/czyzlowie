package pl.matkmiec.backend.weather.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.matkmiec.backend.weather.dto.ImgwMeteoResponseDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ImgwApiClientLiveTest {

    @Autowired
    private ImgwApiClient imgwApiClient;

    @Test
    void shouldFetchMeteo() {
        List<ImgwMeteoResponseDto> result = imgwApiClient.fetchMeteo();
        assertThat(result).isNotNull();
    }
}
