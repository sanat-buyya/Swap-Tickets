package com.example.SwapTicket.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CloudinaryConfig {

	@Value("${cloudinary.api.key}")
	private String cloudinary_key;
	@Value("${cloudinary.api.secret}")
	private String cloudinary_secret;
	@Value("${cloudinary.api.name}")
	private String cloudinary_name;

	@Bean
	Cloudinary cloudinary() {
		String url = "cloudinary://" + cloudinary_key + ":" + cloudinary_secret + "@" + cloudinary_name;
		return new Cloudinary(url);
	}

}