package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.AirlineCreateRequestDto;
import com.flightmanagement.reference.dto.response.AirlineResponseDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.entity.Airline;
import com.flightmanagement.reference.exception.DuplicateReferenceException;
import com.flightmanagement.reference.exception.ReferenceNotFoundException;
import com.flightmanagement.reference.mapper.AirlineMapper;
import com.flightmanagement.reference.repository.AirlineRepository;
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
public class AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirlineMapper airlineMapper;
    private final EventPublishService eventPublishService;

    public PagedResponse<AirlineResponseDto> getAllAirlines(Pageable pageable) {
        Page<Airline> airlines = airlineRepository.findAll(pageable);
        return createPagedResponse(airlines);
    }

    @Cacheable(value = "airlines", key = "#id")
    public AirlineResponseDto getAirlineById(Long id) {
        Airline airline = getAirline(id);
        return airlineMapper.toResponseDto(airline);
    }

    public AirlineResponseDto createAirline(AirlineCreateRequestDto request) {
        if (airlineRepository.existsByCode(request.getCode())) {
            throw new DuplicateReferenceException("Airline code already exists: " + request.getCode());
        }

        Airline airline = airlineMapper.toEntity(request);
        airline = airlineRepository.save(airline);

        eventPublishService.publishReferenceEvent("CREATED", "AIRLINE", airline.getId(), null);
        return airlineMapper.toResponseDto(airline);
    }

    @CacheEvict(value = "airlines", key = "#id")
    public AirlineResponseDto updateAirline(Long id, AirlineCreateRequestDto request) {
        Airline airline = getAirline(id);

        if (!airline.getCode().equals(request.getCode()) && airlineRepository.existsByCode(request.getCode())) {
            throw new DuplicateReferenceException("Airline code already exists: " + request.getCode());
        }

        airlineMapper.updateEntityFromDto(request, airline);
        airline = airlineRepository.save(airline);

        eventPublishService.publishReferenceEvent("UPDATED", "AIRLINE", airline.getId(), null);
        return airlineMapper.toResponseDto(airline);
    }

    @CacheEvict(value = "airlines", key = "#id")
    public void deleteAirline(Long id) {
        Airline airline = getAirline(id);
        airline.setIsActive(false);
        airlineRepository.save(airline);

        eventPublishService.publishReferenceEvent("DELETED", "AIRLINE", airline.getId(), null);
    }

    private Airline getAirline(Long id) {
        return airlineRepository.findById(id)
                .orElseThrow(() -> new ReferenceNotFoundException("Airline not found with id: " + id));
    }

    private PagedResponse<AirlineResponseDto> createPagedResponse(Page<Airline> page) {
        return PagedResponse.<AirlineResponseDto>builder()
                .content(page.getContent().stream().map(airlineMapper::toResponseDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}