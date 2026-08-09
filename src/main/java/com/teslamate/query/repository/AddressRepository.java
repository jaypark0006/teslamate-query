package com.teslamate.query.repository;

import com.teslamate.query.dao.AddressDao;
import com.teslamate.query.dto.AddressDto;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class AddressRepository {

    private final AddressDao addressDao;

    public AddressRepository(AddressDao addressDao) {
        this.addressDao = addressDao;
    }

    public Optional<AddressDto> findById(long id) {
        return addressDao.findById(id);
    }

    public List<AddressDto> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return addressDao.findByIds(ids);
    }
}
