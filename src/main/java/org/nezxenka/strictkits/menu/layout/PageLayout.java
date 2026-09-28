package org.nezxenka.strictkits.menu.layout;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageLayout {

    public static final int ROW_SIZE = 9;
    public static final int NO_SLOT = -1;
    private static final int MAX_ROWS = 6;
    private static final int MIN_PAGED_ROWS = 2;

    private final int page;
    private final int totalPages;
    private final int capacity;
    private final int size;

    public static PageLayout of(int itemCount, int configuredRows, int requestedPage) {
        int capacity = Math.max(ROW_SIZE, (configuredRows - 1) * ROW_SIZE);
        int totalPages = Math.max(1, (itemCount + capacity - 1) / capacity);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        int size = totalPages > 1
                ? Math.max(MIN_PAGED_ROWS, configuredRows) * ROW_SIZE
                : rowsFor(itemCount) * ROW_SIZE;
        return new PageLayout(page, totalPages, capacity, size);
    }

    public boolean isPaged() {
        return totalPages > 1;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages;
    }

    public int offset() {
        return (page - 1) * capacity;
    }

    public int itemsOnPage(int itemCount) {
        return Math.max(0, Math.min(capacity, itemCount - offset()));
    }

    public int previousSlot() {
        return isPaged() ? size - ROW_SIZE : NO_SLOT;
    }

    public int exitSlot() {
        return isPaged() ? size - ROW_SIZE + ROW_SIZE / 2 : NO_SLOT;
    }

    public int nextSlot() {
        return isPaged() ? size - 1 : NO_SLOT;
    }

    private static int rowsFor(int count) {
        return Math.min(MAX_ROWS, Math.max(1, (count + ROW_SIZE - 1) / ROW_SIZE));
    }
}
