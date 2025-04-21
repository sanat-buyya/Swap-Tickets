package com.example.SwapTicket.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.url}")
    private String cloudinaryUrl;  // e.g. "cloudinary://API_KEY:API_SECRET@CLOUD_NAME"

//cloudinary.url=cloudinary://733189736643147:BIaTjViHLyGSiszZCH0R2rqKyRU@dgrxmavho
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dgrxmavho",
            "api_key", "733189736643147",
            "api_secret", "BIaTjViHLyGSiszZCH0R2rqKyRU",
            "secure", true
        ));
    }

}
