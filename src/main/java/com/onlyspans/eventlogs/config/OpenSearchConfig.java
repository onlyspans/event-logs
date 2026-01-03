package com.onlyspans.eventlogs.config;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "opensearch")
public class OpenSearchConfig {

    @Value("${opensearch.host:localhost}")
    private String host;

    @Value("${opensearch.port:9200}")
    private int port;

    @Value("${opensearch.scheme:http}")
    private String scheme;

    @Bean(destroyMethod = "close")
    public RestHighLevelClient openSearchClient() {
        return new RestHighLevelClient(
            RestClient.builder(new HttpHost(scheme, host, port))
        );
    }
}

