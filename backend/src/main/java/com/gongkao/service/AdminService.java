package com.gongkao.service;

import com.gongkao.dto.PaperImportRequest;
import com.gongkao.entity.MaterialGroup;
import com.gongkao.entity.Paper;
import com.gongkao.entity.Question;
import com.gongkao.entity.Region;
import com.gongkao.mapper.MaterialGroupMapper;
import com.gongkao.mapper.PaperMapper;
import com.gongkao.mapper.QuestionMapper;
import com.gongkao.mapper.RegionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final PaperMapper paperMapper;
    private final MaterialGroupMapper materialGroupMapper;
    private final QuestionMapper questionMapper;
    private final RegionMapper regionMapper;

    @Transactional
    public Long importPaper(PaperImportRequest req) {
        // 0. Resolve region
        Integer regionId = req.getPaper().getRegionId();
        String regionName = req.getPaper().getRegionName();
        if (regionId == null && regionName != null && !regionName.isBlank()) {
            Region existing = regionMapper.selectOne(
                    new LambdaQueryWrapper<Region>().eq(Region::getName, regionName));
            if (existing != null) {
                regionId = existing.getId();
            } else {
                Region r = new Region();
                r.setName(regionName);
                r.setCategory("provincial");
                r.setSortOrder(0);
                regionMapper.insert(r);
                regionId = r.getId();
            }
        }

        // 1. Create paper
        Paper paper = new Paper();
        paper.setTitle(req.getPaper().getTitle());
        paper.setYear(req.getPaper().getYear());
        paper.setCategory(req.getPaper().getCategory());
        paper.setRegionId(regionId);
        paper.setQuestionCount(req.getQuestions() != null ? req.getQuestions().size() : 0);
        paperMapper.insert(paper);

        Long paperId = paper.getId();
        log.info("Created paper id={}, title={}", paperId, paper.getTitle());

        // 2. Create material groups, build temp index -> real ID mapping
        Map<Integer, Long> mgIndexToId = new HashMap<>();
        if (req.getMaterialGroups() != null) {
            for (int i = 0; i < req.getMaterialGroups().size(); i++) {
                PaperImportRequest.MaterialGroupItem item = req.getMaterialGroups().get(i);
                MaterialGroup mg = new MaterialGroup();
                mg.setPaperId(paperId);
                mg.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : i + 1);
                mg.setTitle(item.getTitle());
                mg.setContent(item.getContent());
                mg.setImages(item.getImages());
                materialGroupMapper.insert(mg);
                mgIndexToId.put(i, mg.getId());
                log.debug("Created material_group id={}", mg.getId());
            }
        }

        // 3. Insert questions
        if (req.getQuestions() != null) {
            for (PaperImportRequest.QuestionItem item : req.getQuestions()) {
                Question q = new Question();
                q.setPaperId(paperId);
                q.setSortOrder(item.getSortOrder());
                q.setModule(item.getModule());
                q.setSubModule(item.getSubModule());
                q.setType(item.getType());
                q.setContent(item.getContent());
                q.setOptions(item.getOptions());
                q.setAnswer(item.getAnswer());
                q.setExplanation(item.getExplanation());
                q.setImages(item.getImages());
                q.setScore(item.getScore());

                // Resolve material_group_id from index
                if (item.getMaterialGroupId() != null && mgIndexToId.containsKey(item.getMaterialGroupId().intValue())) {
                    q.setMaterialGroupId(mgIndexToId.get(item.getMaterialGroupId().intValue()));
                }

                questionMapper.insert(q);
            }
        }

        log.info("Imported {} questions, {} material groups for paper {}",
                req.getQuestions() != null ? req.getQuestions().size() : 0,
                req.getMaterialGroups() != null ? req.getMaterialGroups().size() : 0,
                paperId);

        return paperId;
    }
}
