package com.example.SwapTicket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class FileService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    
    @Value("${app.train.data.file:classpath:Train_details_22122017.csv}")
    private String trainDataFile;
    
    @Value("${app.upload.directory:uploads/}")
    private String uploadDirectory;
    
    /**
     * Read train data from configured CSV file
     */
    public List<Map<String, String>> readTrainStations() {
        List<Map<String, String>> stations = new ArrayList<>();
        
        try {
            Resource resource = new ClassPathResource("Train_details_22122017.csv");
            
            if (!resource.exists()) {
                logger.error("Train data file not found: {}", trainDataFile);
                return stations;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line = reader.readLine(); // Skip header
                
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split(",", -1);
                    if (columns.length >= 5) {
                        String code = columns[3].trim().toUpperCase();
                        String name = columns[4].trim().toUpperCase();
                        
                        if (!code.isEmpty() && !name.isEmpty()) {
                            Map<String, String> stationMap = new HashMap<>();
                            stationMap.put("code", code);
                            stationMap.put("name", name);
                            stations.add(stationMap);
                        }
                    }
                }
            }
            
            logger.info("Successfully loaded {} train stations", stations.size());
            
        } catch (IOException e) {
            logger.error("Error reading train data file: {}", e.getMessage(), e);
        }
        
        return stations;
    }
    
    /**
     * Get the configured train data file path
     */
    public String getTrainDataFilePath() {
        try {
            Resource resource = new ClassPathResource("Train_details_22122017.csv");
            if (resource.exists()) {
                return resource.getFile().getAbsolutePath();
            }
        } catch (IOException e) {
            logger.warn("Could not get absolute path for train data file: {}", e.getMessage());
        }
        return trainDataFile;
    }
    
    /**
     * Validate and create upload directory if it doesn't exist
     */
    public Path getUploadDirectory() {
        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
            return uploadPath;
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize upload directory", e);
        }
    }
    
    /**
     * Validate file path to prevent directory traversal attacks
     */
    public boolean isValidFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return false;
        }
        
        // Check for directory traversal patterns
        String normalizedPath = filePath.replace("\\", "/");
        return !normalizedPath.contains("../") && 
               !normalizedPath.contains("..\\") &&
               !normalizedPath.startsWith("/") &&
               !normalizedPath.contains(":");
    }
}