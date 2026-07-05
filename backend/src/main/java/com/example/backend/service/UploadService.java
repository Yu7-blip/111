package com.example.backend.service;

import com.example.backend.common.Result;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    Result<?> uploadImage(MultipartFile file);
}
