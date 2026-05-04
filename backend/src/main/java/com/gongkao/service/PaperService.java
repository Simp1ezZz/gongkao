package com.gongkao.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gongkao.dto.*;
import com.gongkao.entity.*;
import com.gongkao.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperMapper paperMapper;
    private final MaterialGroupMapper materialGroupMapper;
    private final QuestionMapper questionMapper;
    private final RegionMapper regionMapper;

    public PageResult<Paper> listPapers(PaperQueryRequest req) {
        String category = req.getCategory() != null ? req.getCategory() : "行测";

        Page<Paper> page = new Page<>(req.getPage(), req.getPageSize());
        IPage<Paper> result = paperMapper.selectPageWithRegion(page, category, req.getRegionId());

        return PageResult.of(result.getRecords(), result.getTotal(), req.getPage(), req.getPageSize());
    }

    public PaperDetailVO getPaperDetail(Long paperId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("试卷不存在: " + paperId);
        }

        if (paper.getRegionId() != null) {
            Region region = regionMapper.selectById(paper.getRegionId());
            if (region != null) {
                paper.setRegionName(region.getName());
            }
        }

        PaperDetailVO vo = new PaperDetailVO();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setCategory(paper.getCategory());
        vo.setRegionName(paper.getRegionName());
        vo.setRating(paper.getRating());
        vo.setQuestionCount(paper.getQuestionCount());
        vo.setYear(paper.getYear());

        LambdaQueryWrapper<MaterialGroup> mgWrapper = new LambdaQueryWrapper<>();
        mgWrapper.eq(MaterialGroup::getPaperId, paperId)
                 .orderByAsc(MaterialGroup::getSortOrder);
        vo.setMaterials(materialGroupMapper.selectList(mgWrapper).stream()
                .map(MaterialGroupVO::from).collect(Collectors.toList()));

        vo.setQuestions(questionMapper.selectByPaperId(paperId).stream()
                .map(QuestionVO::from).collect(Collectors.toList()));

        return vo;
    }

    public List<MaterialGroupVO> getMaterials(Long paperId) {
        LambdaQueryWrapper<MaterialGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialGroup::getPaperId, paperId)
               .orderByAsc(MaterialGroup::getSortOrder);
        return materialGroupMapper.selectList(wrapper).stream()
                .map(MaterialGroupVO::from).collect(Collectors.toList());
    }

    public List<QuestionVO> getQuestionsByKnowledge(String module, String subModule,
                                                     String knowledgePoint, int limit) {
        List<Long> allIds = questionMapper.selectIdsByKnowledge(module, subModule, knowledgePoint);
        if (allIds.isEmpty()) return List.of();

        Collections.shuffle(allIds);
        List<Long> selectedIds = allIds.subList(0, Math.min(limit, allIds.size()));
        List<Question> questions = questionMapper.selectBatchIds(selectedIds);
        return questions.stream().map(QuestionVO::from).collect(Collectors.toList());
    }
}
