package com.eneik.generated.util;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class IdProvider {
    private UUID fixedUuid = null;
    private String fixedStringId = null;

    public UUID generateUuid() {
        if (fixedUuid != null) {
            return fixedUuid;
        }
        return UUID.randomUUID();
    }

    public String generateNotificationId() {
        if (fixedStringId != null) {
            return fixedStringId;
        }
        return "notif_" + UUID.randomUUID().toString().replace("-", "");
    }

    public void setFixedUuid(UUID fixedUuid) {
        this.fixedUuid = fixedUuid;
    }

    public void setFixedStringId(String fixedStringId) {
        this.fixedStringId = fixedStringId;
    }

    public void reset() {
        this.fixedUuid = null;
        this.fixedStringId = null;
    }
}
