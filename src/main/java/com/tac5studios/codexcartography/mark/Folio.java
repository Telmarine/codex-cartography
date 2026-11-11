package com.tac5studios.codexcartography.mark;

/**
 * A folio: a folder of marks.
 */
public final class Folio {

    public String id;
    public String name;
    public int color;
    public boolean shown = true;
    /** True = every mark in it shows in the folio's color; false = just the folio. */
    public boolean colorAll;

    public Folio(String id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }
}
