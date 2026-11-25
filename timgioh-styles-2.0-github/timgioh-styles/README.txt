timgioh styles 2.0 - jeden Eintrag einzeln einstellen
======================================================

BAUEN (Windows)
  1. Java 21 muss installiert sein.
  2. ZIP entpacken, build.bat doppelklicken (braucht Internet, dauert beim ersten Mal ein paar Minuten).
  3. Fertig: build\libs\timgioh-styles-2.0.jar  ->  in den mods-Ordner legen.
     Die alte Datei timgioh-styles-1.0.0.jar vorher aus dem mods-Ordner loeschen.
  (Linux/Mac: ./build.sh)

BENUTZEN
  - Button "timgioh styles" oben links im Titel-, Pause- und Ressourcenpaket-Menue.
  - Liste wie im Screenshot: Kategorien (CRYSTAL PVP, MACE, WEAPONS & TOOLS, ARMOUR, RANGED,
    HEALING & ITEMS, WORLD, INTERFACE, EXTRAS), pro Eintrag eine Pill.
  - Pill anklicken -> Liste mit allen Looks fuer diesen Eintrag -> einen waehlen.
  - "Farbe: ..." waehlt die Packfarbe fuer die Looks "timgioh", "White Warden" und "Flach".
  - "Alles Vanilla" setzt alles zurueck. "Uebernehmen" baut das Pack und laedt die Texturen neu.
  - Die Auswahl wird gespeichert (config/timgioh_styles.properties).

LOOKS PRO EINTRAG
  Vanilla                         immer
  timgioh / White Warden          wo deine Packs Dateien dafuer haben (z.B. End crystal, Totem, Pearls, Cobwebs, HUD)
  Flach (nur Obsidian)            flacher schwarzer Obsidian mit Rahmen in der Packfarbe (+ Crying Obsidian)
  Blutrot, Eisblau, Toxic, Gold, Schatten   5 eigene Farbstile fuer JEDEN Eintrag

  Die 5 Farbstile entstehen beim Uebernehmen aus den Vanilla-Texturen deines Minecraft (Farbverlauf
  dunkel -> hell, Transparenz bleibt). Dadurch passen sie zu jeder Textur, auch bei Ruestung,
  Partikeln, Inventar und Effekten.

HINWEISE
  - Ruestung: Helmets/Chestplates/Boots teilen sich im Spiel eine Textur pro Material. Die Mod faerbt
    dafuer nur den jeweiligen Bereich (Kopf / Koerper+Arme / Beine) um, so geht es trotzdem einzeln.
  - Wenn ein Eintrag im Spiel unveraendert bleibt, gibt es die Textur in deiner Minecraft-Version
    evtl. unter einem anderen Namen (steht dann in logs/latest.log: "keine Vanilla-Dateien gefunden").
  - Das Pack liegt als resourcepacks/timgioh_styles_mix_<nummer>.zip; alte Versionen werden beim
    naechsten Start geloescht.
  - Beim Bauen holt gradle die neueste Yarn- und Fabric-API-Version fuer 1.21.11 selbst. Falls das
    scheitert, in gradle.properties yarn_mappings und fabric_version eintragen.
