package com.eneik.generated.service;

import com.eneik.generated.dto.MaxNotificationRequest;
import com.eneik.generated.dto.TelegramNotificationRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class NotificationDispatcher {

    private final List<TelegramNotificationRequest> dispatchedTelegram = new CopyOnWriteArrayList<>();
    private final List<MaxNotificationRequest> dispatchedMax = new CopyOnWriteArrayList<>();

    public void dispatchTelegram(TelegramNotificationRequest request) {
        dispatchedTelegram.add(request);
        // In a production environment, this would integrate with the actual Telegram Bot API.
    }

    public void dispatchMax(MaxNotificationRequest request) {
        dispatchedMax.add(request);
        // In a production environment, this would integrate with the actual Max messenger API.
    }

    public List<TelegramNotificationRequest> getDispatchedTelegram() {
        return dispatchedTelegram;
    }

    public List<MaxNotificationRequest> getDispatchedMax() {
        return dispatchedMax;
    }

    public void clear() {
        dispatchedTelegram.clear();
        dispatchedMax.clear();
    }
}
