package com.rideapp.dispatch.job;

import com.rideapp.dispatch.entity.DriverOffer;
import com.rideapp.dispatch.entity.OfferStatus;
import com.rideapp.dispatch.repository.DriverOfferRepository;
import com.rideapp.dispatch.service.DriverDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OfferExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OfferExpiryJob.class);

    private final DriverOfferRepository driverOfferRepository;
    private final DriverDispatchService driverDispatchService;

    public OfferExpiryJob(DriverOfferRepository driverOfferRepository, DriverDispatchService driverDispatchService) {
        this.driverOfferRepository = driverOfferRepository;
        this.driverDispatchService = driverDispatchService;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void expireStaleOffers() {
        List<DriverOffer> expiredOffers = driverOfferRepository.findExpiredOffers(Instant.now());
        for (DriverOffer offer : expiredOffers) {
            log.info("Expiring stale offer {} for ride {} on driver {}",
                    offer.getId(), offer.getRide().getId(), offer.getDriver().getId());

            offer.setStatus(OfferStatus.EXPIRED);
            offer.setRespondedAt(Instant.now());
            driverOfferRepository.save(offer);

            driverDispatchService.triggerDriverReplacement(
                    offer.getRide().getId(),
                    offer.getDriver().getId(),
                    "Offer expired without driver response",
                    offer.getAttemptNumber()
            );
        }
    }
}
