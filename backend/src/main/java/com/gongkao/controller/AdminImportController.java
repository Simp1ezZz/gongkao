package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.PaperImportRequest;
import com.gongkao.service.AdminImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminImportController {

    private final AdminImportService adminImportService;

    @PostMapping("/import/confirm")
    public Result<Map<String, Object>> confirmImport(@RequestBody PaperImportRequest req) {
        if (req.getMetadata() == null || req.getSections() == null) {
            return Result.fail("Missing metadata or sections");
        }
        try {
            Map<String, Object> result = adminImportService.confirmImport(req);
            return Result.ok(result);
        } catch (Exception e) {
            return Result.fail("导入失败: " + e.getMessage());
        }
    }
}
