package org.nezxenka.strictkits.storage.sql.query;

import lombok.Getter;

@Getter
public final class SqlQueries {

    private final String cooldownTable;
    private final String claimTable;
    private final String selectCooldowns;
    private final String selectClaims;
    private final String deleteKitCooldowns;
    private final String deleteKitClaims;
    private final String purgeCooldowns;

    public SqlQueries(String tablePrefix) {
        this.cooldownTable = tablePrefix + "cooldowns";
        this.claimTable = tablePrefix + "claims";
        this.selectCooldowns = "SELECT kit, used_at FROM " + cooldownTable + " WHERE uuid = ?";
        this.selectClaims = "SELECT kit FROM " + claimTable + " WHERE uuid = ?";
        this.deleteKitCooldowns = "DELETE FROM " + cooldownTable + " WHERE kit = ?";
        this.deleteKitClaims = "DELETE FROM " + claimTable + " WHERE kit = ?";
        this.purgeCooldowns = "DELETE FROM " + cooldownTable + " WHERE kit = ? AND used_at < ?";
    }
}
