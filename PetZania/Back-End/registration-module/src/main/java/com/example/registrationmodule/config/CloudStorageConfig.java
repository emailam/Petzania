package com.example.registrationmodule.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.aws")
@Profile("!dev")
public class CloudStorageConfig {
    private String accessKey;
    private String secretAccessKey;
    private String region;
    private String bucketName;
    private String cdnUrl;
    private Map<String, Long> maxSize;
    private List<String> allowedTypes;
    private AmazonS3 amazonS3;

    @PostConstruct
    public void init() {
        System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretAccessKey);
        amazonS3 = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
    @Bean
    public AmazonS3 amazonS3() {
        return amazonS3;
    }
}
