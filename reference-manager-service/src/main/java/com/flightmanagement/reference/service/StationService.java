package com.flightmanagement.reference.service;

import com.flightmanagement.reference.dto.request.StationCreateRequestDto;
import com.flightmanagement.reference.dto.response.PagedResponse;
import com.flightmanagement.reference.dto.response.StationResponseDto;
import com.flightmanagement.reference.entity.Station;
import com.flightmanagement.reference.exception.DuplicateReferenceException;
import com.flightmanagement.reference.exception.ReferenceNotFoundException;
import com.flightmanagement.reference.mapper.StationMapper;
import com.flightmanagement.reference.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StationService {

    private final StationRepository stationRepository;
    private final StationMapper stationMapper;
    private final EventPublishService eventPublishService;

    public PagedResponse<StationResponseDto> getAllStations(Pageable pageable) {
        Page<Station> stations = stationRepository.findAll(pageable);
        return createPagedResponse(stations);
    }

    @Cacheable(value = "stations", key = "#id")
    public StationResponseDto getStationById(Long id) {
        Station station = getStation(id);
        return stationMapper.toResponseDto(station);
    }

    public List<StationResponseDto> searchStations(String query) {
        List<Station> stations = stationRepository.searchStations(query.toUpperCase());
        return stations.stream().map(stationMapper::toResponseDto).toList();
    }

    public StationResponseDto createStation(StationCreateRequestDto request) {
        validateUniqueConstraints(request, null);

        Station station = stationMapper.toEntity(request);
        station = stationRepository.save(station);

        eventPublishService.publishReferenceEvent("CREATED", "STATION", station.getId(), null);
        return stationMapper.toResponseDto(station);
    }

    @CacheEvict(value = "stations", key = "#id")
    public StationResponseDto updateStation(Long id, StationCreateRequestDto request) {
        Station station = getStation(id);
        validateUniqueConstraints(request, id);

        stationMapper.updateEntityFromDto(request, station);
        station = stationRepository.save(station);

        eventPublishService.publishReferenceEvent("UPDATED", "STATION", station.getId(), null);
        return stationMapper.toResponseDto(station);
    }

    @CacheEvict(value = "stations", key = "#id")
    public void deleteStation(Long id) {
        Station station = getStation(id);
        station.setIsActive(false);
        stationRepository.save(station);

        eventPublishService.publishReferenceEvent("DELETED", "STATION", station.getId(), null);
    }

    private void validateUniqueConstraints(StationCreateRequestDto request, Long excludeId) {
        Station existingByIcao = stationRepository.findByIcaoCode(request.getIcaoCode()).orElse(null);
        if (existingByIcao != null && !existingByIcao.getId().equals(excludeId)) {
            throw new DuplicateReferenceException("ICAO code already exists: " + request.getIcaoCode());
        }

        if (request.getIataCode() != null) {
            Station existingByIata = stationRepository.findByIataCode(request.getIataCode()).orElse(null);
            if (existingByIata != null && !existingByIata.getId().equals(excludeId)) {
                throw new DuplicateReferenceException("IATA code already exists: " + request.getIataCode());
            }
        }
    }

    private Station getStation(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ReferenceNotFoundException("Station not found with id: " + id));
    }

    private PagedResponse<StationResponseDto> createPagedResponse(Page<Station> page) {
        return PagedResponse.<StationResponseDto>builder()
                .content(page.getContent().stream().map(stationMapper::toResponseDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}