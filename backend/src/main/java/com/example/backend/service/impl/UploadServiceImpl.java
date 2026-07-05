package com.example.backend.service.impl;

import com.example.backend.common.Result;
import com.example.backend.service.UploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final String UPLOAD_DIR = "./uploads/";

    @Override
    public Result<?> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("文件为空");
        }

        try {
            // Ensure upload directory exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate UUID filename with original extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            File destFile = new File(UPLOAD_DIR + filename);
            file.transferTo(destFile);

            // Return URL
            Map<String, Object> data = new HashMap<>();
            data.put("url", "/uploads/" + filename);
            return Result.ok(data);
        } catch (IOException e) {
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }
}
