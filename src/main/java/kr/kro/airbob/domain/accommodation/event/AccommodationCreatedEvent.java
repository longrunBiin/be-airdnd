package kr.kro.airbob.domain.accommodation.event;

import lombok.Builder;

@Builder
public class AccommodationCreatedEvent {
    private final String name;
    private final String thumbnailUrl;
    private final Integer pricePerNight;
    private final Integer maxOccupancy;
    private final Double averageRating;
    private final Integer reviewCount;
}
