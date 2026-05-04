package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String objectKey = "uploads/" + UUID.randomUUID() + ext;

            String key = fileService.upload(
                    objectKey,
                    file.getInputStream(),
                    file.getContentType(),
                    file.getSize());

            String url = fileService.getPresignedUrl(key);

            Map<String, String> result = new HashMap<>();
            result.put("key", key);
            result.put("url", url);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }
}
