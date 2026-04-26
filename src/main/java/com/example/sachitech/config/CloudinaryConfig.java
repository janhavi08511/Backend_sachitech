package com.example.sachitech.config;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

 @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dek24uvqz",
                "api_key", "846826288782234",
                "api_secret", "e4WUuMLFvRufX2B2KtbEy7-_RAk"
        ));
    }
}
