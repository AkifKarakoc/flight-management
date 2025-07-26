package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.AircraftCreateRequestDto;
import com.flightmanagement.reference.dto.response.AircraftResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.entity.Aircraft;
import com.flightmanagement.reference.exception.ReferenceNotFoundException;
import com.flightmanagement.reference.mapper.AircraftMapper;
import com.flightmanagement.reference.repository.AircraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AircraftMapper aircraftMapper;
    private final EventPublishService eventPublishService;

    public PagedResponse<AircraftResponseDto> getAllAircraft(Pageable pageable) {
        Page<Aircraft> aircraft = aircraftRepository.findAll(pageable);
        return createPagedResponse(aircraft);
    }

    @Cacheable(value = "aircraft", key = "#id")
    public AircraftResponseDto getAircraftById(Long id) {
        Aircraft aircraft = getAircraft(id);
        return aircraftMapper.toResponseDto(aircraft);
    }

    public AircraftResponseDto createAircraft(AircraftCreateRequestDto request) {
        Aircraft aircraft = aircraftMapper.toEntity(request);
        aircraft = aircraftRepository.save(aircraft);

        eventPublishService.publishReferenceEvent("CREATED", "AIRCRAFT", aircraft.getId(), null);
        return aircraftMapper.toResponseDto(aircraft);
    }

    @CacheEvict(value = "aircraft", key = "#id")
    public AircraftResponseDto updateAircraft(Long id, AircraftCreateRequestDto request) {
        Aircraft aircraft = getAircraft(id);
        aircraftMapper.updateEntityFromDto(request, aircraft);
        aircraft = aircraftRepository.save(aircraft);

        eventPublishService.publishReferenceEvent("UPDATED", "AIRCRAFT", aircraft.getId(), null);
        return aircraftMapper.toResponseDto(aircraft);
    }

    @CacheEvict(value = "aircraft", key = "#id")
    public void deleteAircraft(Long id) {
        Aircraft aircraft = getAircraft(id);
        aircraft.setIsActive(false);
        aircraftRepository.save(aircraft);

        eventPublishService.publishReferenceEvent("DELETED", "AIRCRAFT", aircraft.getId(), null);
    }

    private Aircraft getAircraft(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new ReferenceNotFoundException("Aircraft not found with id: " + id));
    }

    private PagedResponse<AircraftResponseDto> createPagedResponse(Page<Aircraft> page) {
        return PagedResponse.<AircraftResponseDto>builder()
                .content(page.getContent().stream().map(aircraftMapper::toResponseDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}