package org.nezxenka.strictkits.player.persistence;

import lombok.RequiredArgsConstructor;
import org.nezxenka.strictkits.player.data.PlayerData;
import org.nezxenka.strictkits.storage.api.StorageProvider;
import org.nezxenka.strictkits.storage.model.DataEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class PlayerDataWriter {

    private final StorageProvider storage;
    private final Logger logger;
    private final AtomicBoolean failing = new AtomicBoolean();
    private final AtomicLong written = new AtomicLong();

    public boolean isFailing() {
        return failing.get();
    }

    public long getWritten() {
        return written.get();
    }

    public List<PlayerData> save(Collection<PlayerData> candidates) {
        List<PendingWrite> pending = new ArrayList<>();
        for (PlayerData data : candidates) {
            if (data.hasUnsaved()) {
                pending.add(PendingWrite.of(data));
            }
        }
        if (pending.isEmpty()) {
            return List.of();
        }
        List<DataEntry> cooldownBatch = new ArrayList<>();
        List<DataEntry> claimBatch = new ArrayList<>();
        pending.forEach(write -> write.appendTo(cooldownBatch, claimBatch));
        try {
            storage.writeCooldowns(cooldownBatch);
            storage.writeClaims(claimBatch);
        } catch (Exception e) {
            if (failing.compareAndSet(false, true)) {
                logger.log(Level.SEVERE, "Не удалось записать данные игроков, запись будет повторена", e);
            }
            return List.of();
        }
        if (failing.compareAndSet(true, false)) {
            logger.info("Запись данных игроков восстановлена");
        }
        written.addAndGet(cooldownBatch.size() + claimBatch.size());
        List<PlayerData> saved = new ArrayList<>(pending.size());
        for (PendingWrite write : pending) {
            write.confirm();
            saved.add(write.data);
        }
        return saved;
    }

    @RequiredArgsConstructor
    private static final class PendingWrite {

        private final PlayerData data;
        private final Map<String, Long> cooldowns;
        private final Map<String, Long> claims;

        static PendingWrite of(PlayerData data) {
            return new PendingWrite(data, data.unsavedCooldowns(), data.unsavedClaims());
        }

        void appendTo(List<DataEntry> cooldownBatch, List<DataEntry> claimBatch) {
            cooldowns.forEach((kitKey, timestamp) -> cooldownBatch.add(new DataEntry(data.getUuid(), kitKey, timestamp)));
            claims.forEach((kitKey, timestamp) -> claimBatch.add(new DataEntry(data.getUuid(), kitKey, timestamp)));
        }

        void confirm() {
            data.markSaved(cooldowns, claims);
        }
    }
}
