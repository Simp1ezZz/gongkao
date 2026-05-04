package com.gongkao.controller;

import com.gongkao.common.Result;
import com.gongkao.dto.*;
import com.gongkao.entity.Paper;
import com.gongkao.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @GetMapping
    public Result<PageResult<Paper>> listPapers(PaperQueryRequest req) {
        return Result.ok(paperService.listPapers(req));
    }

    @GetMapping("/{id}")
    public Result<PaperDetailVO> getPaperDetail(@PathVariable Long id) {
        return Result.ok(paperService.getPaperDetail(id));
    }

    @GetMapping("/{id}/materials")
    public Result<List<MaterialGroupVO>> getMaterials(@PathVariable Long id) {
        return Result.ok(paperService.getMaterials(id));
    }

    @GetMapping("/questions/by-knowledge")
    public Result<List<QuestionVO>> getQuestionsByKnowledge(
            @RequestParam String module,
            @RequestParam(required = false) String sub_module,
            @RequestParam(required = false) String knowledge_point,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(paperService.getQuestionsByKnowledge(
                module, sub_module, knowledge_point, limit));
    }
}
