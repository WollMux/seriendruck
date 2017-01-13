package de.muenchen.mailmerge.print.model;

import de.muenchen.mailmerge.print.PrintFunction;

/**
 * Jedes hier definierte konkrete PrintModel definiert dieses Interface und kann
 * (ausschließlich) innerhalb der Java-VM des WollMux verwendet werden um auf nicht
 * im XPrintModel exportierte Methode der PrintModels zuzugreifen.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public interface InternalPrintModel
{
  /**
   * Lädt die WollMux-interne Druckfunktion printFunction (muss als
   * PrintFunction-Objekt vorliegen) in das XPrintModel und ordnet sie gemäß dem
   * ORDER-Attribut an der richtigen Position in die Aufrufkette der zu
   * bearbeitenden Druckfunktionen ein.
   * 
   * @param printFunction
   *          Druckfunktion, die durch das PrintModel verwaltet werden soll.
   * @return liefert true, wenn die Druckfunktion erfolgreich in die Aufrufkette
   *         übernommen wurde oder bereits geladen war und false, wenn die
   *         Druckfunktion aufgrund vorangegangener Fehler nicht in die Aufrufkette
   *         aufgenommen werden konnte.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public boolean useInternalPrintFunction(PrintFunction printFunction);
}