package dev.nexus.cosmetics;

import dev.nexus.module.Module;

import java.util.List;

/** Kosmetyk z lista opcji (wbudowane + pliki uzytkownika); modul = jedno zrodlo prawdy dla GUI i menu Cosmetics. */
public interface Cosmetic {
    List<String> options();
    List<String> builtins();
    String selected();
    void select(String name);
    void refresh();
    String folderHint();

    default Module module() { return (Module) this; }
}
