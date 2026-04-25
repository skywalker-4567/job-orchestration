package com.jobplatform.controlplane.assignment;

import com.jobplatform.controlplane.enums.Region;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RegionAssignmentService {

    private static final Region[] REGIONS = {
            Region.US_EAST,
            Region.EU_WEST,
            Region.AP_SOUTH
    };

    private final AtomicInteger index = new AtomicInteger(0);

    public Region assignNextRegion() {
        int current = index.getAndUpdate(i -> (i + 1) % REGIONS.length);
        return REGIONS[current];
    }
}