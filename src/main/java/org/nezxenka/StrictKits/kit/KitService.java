package org.nezxenka.StrictKits.kit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.nezxenka.StrictKits.config.Messages;
import org.nezxenka.StrictKits.config.Settings;
import org.nezxenka.StrictKits.gui.MenuHolder;
import org.nezxenka.StrictKits.player.PlayerData;
import org.nezxenka.StrictKits.player.PlayerDataManager;
import org.nezxenka.StrictKits.util.GUItems;
import org.nezxenka.StrictKits.util.Messenger;
import org.nezxenka.StrictKits.util.TimeFormat;

public final class KitService {

    private final KitManager kits;
    private final PlayerDataManager players;
    private final Messages messages;
    private final Settings settings;

    public KitService(KitManager kits, PlayerDataManager players, Messages messages, Settings settings) {
        this.kits = kits;
        this.players = players;
        this.messages = messages;
        this.settings = settings;
    }

    public long remainingCooldown(PlayerData data, Kit kit) {
        long last = data.getCooldown(kit.getKey());
        if (last == 0L) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - last;
        long remaining = kit.getCooldownMillis() - elapsed;
        return remaining > 0L ? remaining : 0L;
    }

    public boolean give(Player player, Kit kit) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null) {
            Messenger.send(player, messages.getDataNotLoaded());
            return false;
        }
        boolean admin = player.hasPermission("strictkits.admin");
        if (!admin) {
            if (!kit.hasAccess(player)) {
                Messenger.send(player, messages.getNoPermission());
                return false;
            }
            if (kit.isOneTimeUse() && data.hasClaim(kit.getKey())) {
                Messenger.send(player, messages.getKitAlreadyClaimed());
                return false;
            }
            if (!kit.isOneTimeUse()) {
                long remaining = remainingCooldown(data, kit);
                if (remaining > 0L) {
                    Messenger.send(player, messages.getCooldown(TimeFormat.getFormattedCooldown(remaining)));
                    return false;
                }
            }
        }
        if (kit.isEmpty()) {
            Messenger.send(player, messages.getKitEmpty());
            return false;
        }
        kit.applyTo(player);
        Messenger.send(player, messages.getKitReceived(kit.getName()));
        if (admin) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (kit.isOneTimeUse()) {
            data.addClaim(kit.getKey(), now);
        } else {
            data.setCooldown(kit.getKey(), now);
        }
        return true;
    }

    public void giveDirect(Player player, Kit kit) {
        if (kit.isEmpty()) {
            Messenger.send(player, messages.getKitEmpty());
            return;
        }
        kit.applyTo(player);
        Messenger.send(player, messages.getKitReceived(kit.getName()));
    }

    public void giveFirstJoinKits(Player player) {
        PlayerData data = players.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (Kit kit : kits.all()) {
            if (!kit.isFirstTimeJoinKit() || kit.isEmpty()) {
                continue;
            }
            if (kit.isOneTimeUse()) {
                if (data.hasClaim(kit.getKey())) {
                    continue;
                }
                kit.applyTo(player);
                data.addClaim(kit.getKey(), now);
            } else {
                kit.applyTo(player);
                data.setCooldown(kit.getKey(), now);
            }
        }
    }

    public void preview(Player player, Kit kit) {
        if (settings.isPreviewRequiresPermission() && !player.hasPermission("strictkits.preview")) {
            Messenger.send(player, messages.getNoPermission());
            return;
        }
        if (kit.isEmpty()) {
            Messenger.send(player, messages.getKitEmpty());
            return;
        }
        MenuHolder holder = MenuHolder.preview(kit);
        Inventory inventory = Bukkit.createInventory(holder, 54, messages.getGuiPreviewTitle(kit.getName()));
        holder.setInventory(inventory);
        ItemStack[] main = kit.getMainContent();
        int limit = Math.min(main.length, 36);
        for (int i = 0; i < limit; i++) {
            if (main[i] != null) {
                inventory.setItem(i, main[i].clone());
            }
        }
        ItemStack[] armor = kit.getArmorContent();
        placeArmor(inventory, armor, 3, 36);
        placeArmor(inventory, armor, 2, 37);
        placeArmor(inventory, armor, 1, 38);
        placeArmor(inventory, armor, 0, 39);
        if (main.length > 40 && main[40] != null) {
            inventory.setItem(40, main[40].clone());
        }
        inventory.setItem(49, GUItems.getExitButton());
        player.openInventory(inventory);
    }

    private void placeArmor(Inventory inventory, ItemStack[] armor, int index, int slot) {
        if (index < armor.length && armor[index] != null) {
            inventory.setItem(slot, armor[index].clone());
        }
    }

    public Messages getMessages() {
        return messages;
    }

    public Settings getSettings() {
        return settings;
    }

    public KitManager getKits() {
        return kits;
    }

    public PlayerDataManager getPlayers() {
        return players;
    }
}
