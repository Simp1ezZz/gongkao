package com.gongkao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongkao.common.Result;
import com.gongkao.entity.Region;
import com.gongkao.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionMapper regionMapper;

    @GetMapping
    public Result<List<Region>> listRegions() {
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Region::getSortOrder);
        return Result.ok(regionMapper.selectList(wrapper));
    }
}
