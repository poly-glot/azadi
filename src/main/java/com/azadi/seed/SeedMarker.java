package com.azadi.seed;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Id;

import java.time.Instant;

@Entity(name = "SeedMarker")
public class SeedMarker {

    @Id
    private Long id;
    private Instant seededAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getSeededAt() {
        return seededAt;
    }

    public void setSeededAt(Instant seededAt) {
        this.seededAt = seededAt;
    }
}
