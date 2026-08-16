package com.teslamate.query.service;

import com.teslamate.query.dao.CarDao;
import com.teslamate.query.dao.DriveDao;
import com.teslamate.query.dao.PositionDao;
import com.teslamate.query.entity.CarEntity;
import com.teslamate.query.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommuteServiceTest {

    @Test
    void acceptsNumericIdOrVin() {
        CarDao cars = mock(CarDao.class);
        CarEntity row = new CarEntity(1L, null, null, "LRWYGCFJ3RC674572", "Y", "Y",
                null, null, null, null, null, null, null, null);
        when(cars.findById(1L)).thenReturn(Optional.of(row));
        when(cars.findByVin("LRWYGCFJ3RC674572")).thenReturn(Optional.of(row));
        CommuteService svc = new CommuteService(cars, mock(DriveDao.class), mock(PositionDao.class));
        assertEquals(1L, svc.requireCarId("1"));
        assertEquals(1L, svc.requireCarId("LRWYGCFJ3RC674572"));
        assertThrows(NotFoundException.class, () -> svc.requireCarId("NOPE"));
    }
}
