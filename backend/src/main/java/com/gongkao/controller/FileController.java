package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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

    @GetMapping("/serve/**")
    public void serveFile(jakarta.servlet.http.HttpServletRequest request,
                          jakarta.servlet.http.HttpServletResponse response) {
        String path = request.getRequestURI().substring("/api/files/serve/".length());
        try (InputStream stream = fileService.getObject(path)) {
            response.setContentType("application/octet-stream");
            response.setHeader("Cache-Control", "public, max-age=86400");
            byte[] buffer = new byte[8192];
            int bytesRead;
            var out = response.getOutputStream();
            while ((bytesRead = stream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        } catch (Exception e) {
            response.setStatus(404);
        }
    }
}
