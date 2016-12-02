package de.muenchen.allg.itd51.wollmux.print;

import java.util.HashMap;

/**
 * Beschreibt einen DataSourceWriter mit dem die Daten des Seriendrucks in eine
 * Datenquelle geschrieben werden können. Eine konkrete Ableitungen ist der
 * {@link CSVDataSourceWriter}.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public interface DataSourceWriter
{
  /**
   * Fügt der zu erzeugenden Datenquelle einen neuen Datensatz hinzu durch
   * Schlüssel/Wert-Paare in einer HashMap definiert ist.
   * 
   * @throws Exception
   *           falls etwas beim Hinzufügen schief geht.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void addDataset(HashMap<String, String> ds) throws Exception;

  /**
   * Nachdem mit {@link #addDataset(HashMap)} alle Datensätze hinzugefügt wurden
   * schließt der Aufruf dieser Methode die Erzeugung der Datenquelle ab. Nach dem
   * Aufruf von {@link #flushAndClose()} ist die Erzeugung abgeschlossen und es
   * darf kein weiterer Aufruf von {@link #addDataset(HashMap)} erfolgen (bzw.
   * dieser ist dann ohne Wirkung).
   * 
   * @throws Exception
   *           falls etwas beim Finalisieren schief geht.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void flushAndClose() throws Exception;

  /**
   * Liefert die Anzahl der (bisher) mit {@link #addDataset(HashMap)} hinzugefügten
   * Datensätze zurück.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public int getSize();

  /**
   * Entscheidet ob aus den PersistentData der Originaldatei die WollMux-Abschitte
   * gelöscht werden müssen.
   * 
   * @return true falls die Abschnitte gelöscht werden müssen, false sonst
   */
  public boolean isAdjustMainDoc();
}