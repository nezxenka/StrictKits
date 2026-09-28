package org.nezxenka.strictkits.message.section;

import lombok.Getter;
import org.nezxenka.strictkits.message.template.Message;
import org.nezxenka.strictkits.message.template.MessageReader;
import org.nezxenka.strictkits.message.template.Placeholders;

@Getter
public final class MenuMessages {

    private final Message title;
    private final Message previewTitle;
    private final String loreAvailable;
    private final Message loreCooldown;
    private final String loreClaimed;
    private final String loreNoPermission;
    private final Message defaultIconName;
    private final String exitButton;
    private final String previousButton;
    private final String nextButton;

    public MenuMessages(MessageReader reader) {
        this.title = reader.template("gui.title", Placeholders.PAGE, Placeholders.PAGES);
        this.previewTitle = reader.template("gui.preview-title", Placeholders.KIT);
        this.loreAvailable = reader.plain("gui.lore-available");
        this.loreCooldown = reader.template("gui.lore-cooldown", Placeholders.COOLDOWN);
        this.loreClaimed = reader.plain("gui.lore-claimed");
        this.loreNoPermission = reader.plain("gui.lore-no-permission");
        this.defaultIconName = reader.template("gui.default-icon-name", Placeholders.KIT);
        this.exitButton = reader.plain("gui.buttons.exit");
        this.previousButton = reader.plain("gui.buttons.previous");
        this.nextButton = reader.plain("gui.buttons.next");
    }
}
