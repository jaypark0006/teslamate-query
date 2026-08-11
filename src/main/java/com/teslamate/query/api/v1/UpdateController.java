package com.teslamate.query.api.v1;

import com.teslamate.query.dao.UpdateDao;
import com.teslamate.query.dto.UpdateDto;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/updates")
@Tag(name = "Updates")
public class UpdateController {

    private final UpdateDao updateDao;
    private final QuerySupport support;

    public UpdateController(UpdateDao updateDao, QuerySupport support) {
        this.updateDao = updateDao;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "Software update history")
    public List<UpdateDto> list(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        var r = support.requireRange(from, to);
        return updateDao.findByCarAndTime(carId, r[0], r[1]);
    }
}
