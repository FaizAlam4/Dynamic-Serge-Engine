package com.surge.service;

import com.surge.model.ZonePricing;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Keep alive forever
        this.emitters.add(emitter);
        
        try {
            // Force Nginx to flush headers immediately by sending a tiny chunk of valid JSON data
            emitter.send(new java.util.ArrayList<>());
        } catch (IOException e) {
            this.emitters.remove(emitter);
        }

        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError((e) -> this.emitters.remove(emitter));
        
        return emitter;
    }

    // Sends the latest pricing data to all connected browsers instantly
    public void sendRealTimeUpdate(List<ZonePricing> pricingData) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(pricingData);
            } catch (IOException e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }
}
