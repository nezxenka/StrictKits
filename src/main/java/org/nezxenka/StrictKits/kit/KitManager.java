package org.nezxenka.StrictKits.kit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class KitManager {

    private final ConcurrentHashMap<String, Kit> kits = new ConcurrentHashMap<>();
    private final KitStorage storage;
    private final Executor ioExecutor;
    private volatile List<Kit> snapshot = Collections.emptyList();
    private volatile List<String> nameSnapshot = Collections.emptyList();

    public KitManager(KitStorage storage, Executor ioExecutor) {
        this.storage = storage;
        this.ioExecutor = ioExecutor;
    }

    public int loadAll() {
        kits.clear();
        for (Kit kit : storage.loadAll()) {
            kits.put(kit.getKey(), kit);
        }
        rebuildSnapshot();
        return kits.size();
    }

    public Kit get(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return kits.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    public List<Kit> all() {
        return snapshot;
    }

    public List<String> names() {
        return nameSnapshot;
    }

    public int size() {
        return kits.size();
    }

    public Kit create(String name) {
        Kit kit = new Kit(name);
        kit.markDirty();
        if (kits.putIfAbsent(kit.getKey(), kit) != null) {
            return null;
        }
        rebuildSnapshot();
        flush(kit);
        return kit;
    }

    public boolean remove(Kit kit) {
        if (kits.remove(kit.getKey(), kit)) {
            rebuildSnapshot();
            ioExecutor.execute(() -> storage.delete(kit));
            return true;
        }
        return false;
    }

    public void flush(Kit kit) {
        if (kit.consumeDirty()) {
            ioExecutor.execute(() -> storage.save(kit));
        }
    }

    public void flushAll() {
        for (Kit kit : snapshot) {
            flush(kit);
        }
    }

    public void flushAllBlocking() {
        for (Kit kit : snapshot) {
            if (kit.consumeDirty()) {
                storage.save(kit);
            }
        }
    }

    private void rebuildSnapshot() {
        Collection<Kit> values = kits.values();
        List<Kit> list = new ArrayList<>(values);
        list.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        List<String> names = new ArrayList<>(list.size());
        for (Kit kit : list) {
            names.add(kit.getName());
        }
        this.snapshot = Collections.unmodifiableList(list);
        this.nameSnapshot = Collections.unmodifiableList(names);
    }

    public KitStorage getStorage() {
        return storage;
    }
}
