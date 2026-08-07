package com.eneik.generated.util;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class TimeProvider {
    private LocalDateTime fixedDateTime = null;

    public LocalDateTime now() {
        if (fixedDateTime != null) {
            return fixedDateTime;
        }
        return LocalDateTime.now();
    }

    public void setFixedDateTime(LocalDateTime fixedDateTime) {
        this.fixedDateTime = fixedDateTime;
    }

    public void reset() {
        this.fixedDateTime = null;
    }
}
