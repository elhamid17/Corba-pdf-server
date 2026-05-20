package sn.ussein.gateway.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import sn.ussein.gateway.model.Job;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends MongoRepository<Job, String> {

    Page<Job> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Job> findByGuestIdOrderByCreatedAtDesc(String guestId);

    long countByUserId(String userId);

    long countByGuestId(String guestId);

    long countByCreatedAtAfter(Instant after);

    long countByUserIdIsNull();

    Page<Job> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
