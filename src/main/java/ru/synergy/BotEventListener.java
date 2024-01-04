package ru.synergy;

import java.util.EventListener;

public interface BotEventListener extends EventListener {
      public void processEvent(BotEvent event);
}