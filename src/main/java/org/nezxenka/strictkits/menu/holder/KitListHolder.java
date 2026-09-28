package org.nezxenka.strictkits.menu.holder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.nezxenka.strictkits.kit.model.Kit;
import org.nezxenka.strictkits.menu.layout.PageLayout;

@RequiredArgsConstructor
public final class KitListHolder extends MenuHolder {

    @Getter
    private final PageLayout layout;
    private final Kit[] kits;

    @Override
    public int getExitSlot() {
        return layout.exitSlot();
    }

    public Kit kitAt(int slot) {
        return slot >= 0 && slot < kits.length ? kits[slot] : null;
    }
}
