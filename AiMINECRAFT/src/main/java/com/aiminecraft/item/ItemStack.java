package com.aiminecraft.item;

/** Stos przedmiotow: id + liczba. */
public class ItemStack {

    public int id;
    public int count;

    public ItemStack(int id, int count) {
        this.id = id;
        this.count = count;
    }

    public ItemStack copy() {
        return new ItemStack(id, count);
    }

    public boolean isEmpty() {
        return count <= 0;
    }

    public Item item() {
        return Item.get(id);
    }

    @Override
    public String toString() {
        Item it = item();
        return (it != null ? it.name : "?") + " x" + count;
    }
}
