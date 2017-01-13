package de.muenchen.mailmerge.print;

import de.muenchen.allg.itd51.wollmux.XPrintModel;

/**
 * Übernimmt das Aktualisieren der Fortschrittsanzeige im XPrintModel pmod.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
class ProgressUpdater
{
  private XPrintModel pmod;

  private int currentCount;

  public final int maxDatasets;

  public ProgressUpdater(XPrintModel pmod, int maxDatasets)
  {
    this.pmod = pmod;
    this.currentCount = 0;
    this.maxDatasets = maxDatasets;
    pmod.setPrintProgressMaxValue((short) maxDatasets);
    pmod.setPrintProgressValue((short) 0);
  }

  public void incrementProgress()
  {
    pmod.setPrintProgressValue((short) ++currentCount);
  }

  public void setMessage(String text)
  {
    this.currentCount = 0;
    pmod.setPrintMessage(text);
  }
}