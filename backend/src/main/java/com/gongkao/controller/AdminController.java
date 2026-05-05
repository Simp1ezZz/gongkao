package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.PaperImportRequest;
import com.gongkao.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/papers/import")
    public Result<Map<String, Object>> importPaper(@RequestBody PaperImportRequest req) {
        Long paperId = adminService.importPaper(req);
        return Result.ok(Map.of("paperId", paperId));
    }
}
