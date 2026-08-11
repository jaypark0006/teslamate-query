package com.teslamate.query.api.v1;

import com.teslamate.query.dao.StateDao;
import com.teslamate.query.dto.StateDto;
import com.teslamate.query.service.QuerySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/states")
@Tag(name = "States")
public class StateController {

    private final StateDao stateDao;
    private final QuerySupport support;

    public StateController(StateDao stateDao, QuerySupport support) {
        this.stateDao = stateDao;
        this.support = support;
    }

    @GetMapping
    @Operation(summary = "Connectivity intervals (online/offline/asleep)")
    public List<StateDto> list(
            @RequestParam long carId,
            @RequestParam String from,
            @RequestParam String to
    ) {
        var r = support.requireRange(from, to);
        return stateDao.findByCarAndTime(carId, r[0], r[1]);
    }
}
