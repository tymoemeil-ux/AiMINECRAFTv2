package com.aiminecraft.item;

/** Ekwipunek: 36 slotow (0-8 hotbar, 9-35 plecak). */
public class Inventory {

    public static final int SIZE = 36;
    public static final int HOTBAR = 9;
    public static final int MAX_STACK = 64;

    private final ItemStack[] slots = new ItemStack[SIZE];
    private int selected = 0;

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = Math.max(0, Math.min(HOTBAR - 1, selected));
    }

    public ItemStack get(int index) {
        return slots[index];
    }

    public void set(int index, ItemStack stack) {
        slots[index] = (stack != null && stack.isEmpty()) ? null : stack;
    }

    public ItemStack heldStack() {
        return slots[selected];
    }

    public int heldId() {
        ItemStack s = heldStack();
        return s != null ? s.id : 0;
    }

    /** Dodaje przedmioty, zwraca liczbe, ktora sie nie zmiescila. */
    public int add(int id, int count) {
        // Najpierw dokladaj do istniejacych stosow.
        for (int i = 0; i < SIZE && count > 0; i++) {
            ItemStack s = slots[i];
            if (s != null && s.id == id && s.count < MAX_STACK) {
                int space = MAX_STACK - s.count;
                int take = Math.min(space, count);
                s.count += take;
                count -= take;
            }
        }
        // Potem nowe stosy.
        for (int i = 0; i < SIZE && count > 0; i++) {
            if (slots[i] == null) {
                int take = Math.min(MAX_STACK, count);
                slots[i] = new ItemStack(id, take);
                count -= take;
            }
        }
        return count;
    }

    /** Zuzywa jeden przedmiot z wybranej reki. Zwraca false, jesli re pusta. */
    public boolean consumeHeld() {
        ItemStack s = slots[selected];
        if (s == null) {
            return false;
        }
        s.count--;
        if (s.count <= 0) {
            slots[selected] = null;
        }
        return true;
    }

    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            slots[i] = null;
        }
    }

    public boolean isEmpty() {
        for (ItemStack s : slots) {
            if (s != null) {
                return false;
            }
        }
        return true;
    }
}
