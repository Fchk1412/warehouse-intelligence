package com.warehouse.intelligence.warehouse;

import com.warehouse.intelligence.warehouse.dto.CreateWarehouseRequest;
import com.warehouse.intelligence.warehouse.dto.UpdateWarehouseRequest;
import com.warehouse.intelligence.warehouse.dto.WarehouseDto;
import com.warehouse.intelligence.warehouse.dto.WarehouseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    private Warehouse findWarehouseOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Warehouse not found"
                ));
    }

    public Page<WarehouseDto> getAllWarehouses(Pageable pageable) {
        return warehouseRepository.findAll(pageable).map(WarehouseMapper::toWarehouseDto);
    }

    public WarehouseDto getWarehouseById(Long id) {
        Warehouse warehouse = findWarehouseOrThrow(id);
        return WarehouseMapper.toWarehouseDto(warehouse);
    }

    public WarehouseDto createWarehouse(CreateWarehouseRequest request) {
        if (warehouseRepository.existsByCode(request.code())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Warehouse code already exists"
            );
        }

        Warehouse warehouse = new Warehouse(
                request.code(),
                request.name(),
                request.location()
        );
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return WarehouseMapper.toWarehouseDto(savedWarehouse);
    }

    public WarehouseDto updateWarehouse(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = findWarehouseOrThrow(id);

        if (warehouseRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Warehouse code already exists"
            );
        }

        warehouse.setCode(request.code());
        warehouse.setName(request.name());
        warehouse.setLocation(request.location());

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return WarehouseMapper.toWarehouseDto(savedWarehouse);
    }

    public WarehouseDto deleteWarehouse(Long id) {
        Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
        return WarehouseMapper.toWarehouseDto(warehouse);
    }
}

