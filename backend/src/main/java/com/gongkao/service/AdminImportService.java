package com.gongkao.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongkao.dto.PaperImportRequest;
import com.gongkao.entity.MaterialGroup;
import com.gongkao.entity.Paper;
import com.gongkao.entity.Question;
import com.gongkao.mapper.MaterialGroupMapper;
import com.gongkao.mapper.PaperMapper;
import com.gongkao.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminImportService {

    private final PaperMapper paperMapper;
    private final MaterialGroupMapper materialGroupMapper;
    private final QuestionMapper questionMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public Map<String, Object> confirmImport(PaperImportRequest req) {
        // 1. Create Paper
        Paper paper = new Paper();
        PaperImportRequest.Metadata meta = req.getMetadata();
        paper.setTitle(meta.getTitle());
        String category = meta.getCategory();
        if (!"行测".equals(category) && !"申论".equals(category)) {
            category = "行测";
        }
        paper.setCategory(category);
        paper.setRegionName(meta.getRegionName());
        paper.setRating(meta.getRating() != null ? meta.getRating() : 0);
        paper.setYear(meta.getYear());
        paperMapper.insert(paper);

        // 2. Create Material Groups
        Map<Integer, Long> materialGroupIndexToId = new HashMap<>();
        if (req.getMaterialGroups() != null) {
            for (int i = 0; i < req.getMaterialGroups().size(); i++) {
                PaperImportRequest.MaterialGroupItem mgItem = req.getMaterialGroups().get(i);
                MaterialGroup mg = new MaterialGroup();
                mg.setPaperId(paper.getId());
                mg.setTitle(mgItem.getTitle());
                mg.setContent(mgItem.getContent());
                mg.setSortOrder(mgItem.getSortOrder() != null ? mgItem.getSortOrder() : i + 1);
                if (mgItem.getImages() != null) {
                    mg.setImages(toJson(mgItem.getImages()));
                }
                materialGroupMapper.insert(mg);
                materialGroupIndexToId.put(i, mg.getId());
            }
        }

        // 3. Build question number → material group id mapping
        Map<Integer, Long> questionNumberToMgId = new HashMap<>();
        if (req.getMaterialGroups() != null) {
            for (int i = 0; i < req.getMaterialGroups().size(); i++) {
                PaperImportRequest.MaterialGroupItem mgItem = req.getMaterialGroups().get(i);
                Long mgId = materialGroupIndexToId.get(i);
                if (mgItem.getQuestionNumbers() != null) {
                    for (Integer qn : mgItem.getQuestionNumbers()) {
                        questionNumberToMgId.put(qn, mgId);
                    }
                }
            }
        }

        // 4. Create Questions
        int totalCount = 0;
        for (PaperImportRequest.SectionItem section : req.getSections()) {
            for (PaperImportRequest.QuestionItem qItem : section.getQuestions()) {
                Question q = new Question();
                q.setPaperId(paper.getId());
                q.setSortOrder(qItem.getSortOrder());
                q.setModule(qItem.getModule() != null ? qItem.getModule() : section.getModule());
                q.setType(qItem.getType() != null ? qItem.getType() : "single_choice");
                q.setContent(qItem.getContent());
                q.setAnswer(qItem.getAnswer());
                q.setExplanation(qItem.getExplanation());
                q.setScore(qItem.getScore() != null ? qItem.getScore() : BigDecimal.ONE);

                if (qItem.getOptions() != null) {
                    q.setOptions(toJson(qItem.getOptions()));
                }
                if (qItem.getImages() != null) {
                    q.setImages(toJson(qItem.getImages()));
                }

                if (qItem.getMaterialGroupIndex() != null && materialGroupIndexToId.containsKey(qItem.getMaterialGroupIndex())) {
                    q.setMaterialGroupId(materialGroupIndexToId.get(qItem.getMaterialGroupIndex()));
                } else if (questionNumberToMgId.containsKey(qItem.getSortOrder())) {
                    q.setMaterialGroupId(questionNumberToMgId.get(qItem.getSortOrder()));
                }

                questionMapper.insert(q);
                totalCount++;
            }
        }

        // 5. Update paper question count
        paper.setQuestionCount(totalCount);
        paperMapper.updateById(paper);

        log.info("Imported paper: id={}, title={}, questions={}", paper.getId(), paper.getTitle(), totalCount);

        Map<String, Object> result = new HashMap<>();
        result.put("paper_id", paper.getId());
        result.put("question_count", totalCount);
        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
