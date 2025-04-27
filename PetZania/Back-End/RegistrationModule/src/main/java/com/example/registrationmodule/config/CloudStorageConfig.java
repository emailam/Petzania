package com.example.registrationmodule.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class CloudStorageConfig {
    @Value("${spring.aws.access-key}")
    private String accessKey;
    @Value("${spring.aws.secret-access-key}")
    private String secretAccessKey;
    @Value("${spring.aws.region}")
    private String region;
    @Value("${spring.aws.bucket.name}")
    private String bucketName;
    private AmazonS3 amazonS3;

    @PostConstruct
    public void init() {
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
