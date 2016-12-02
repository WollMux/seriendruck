package de.muenchen.allg.itd51.wollmux.print;

import de.muenchen.allg.itd51.wollmux.core.db.Dataset;

/**
 * Wrapper für ein Dataset, um es einerseits in eine JList packen zu können,
 * andererseits auch dafür, den Zustand ausgewählt oder nicht speichern zu können.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
class ListElement
{
  private Dataset ds;

  private boolean selected = false;

  private String description;

  /**
   * Initialisiert dieses ListElement mit dem Dataset ds, wobei falls vorhanden die
   * Spalten "WollMuxDescription" und "WollMuxSelected" ausgewertet werden, um den
   * toString() respektive isSelected() Wert zu bestimmen. Falls keine
   * WollMuxDescription vorhanden ist, so wird description verwendet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ListElement(Dataset ds, String description)
  {
    this.ds = ds;
    this.description = description;
    try
    {
      String des = ds.get("WollMuxDescription");
      if (des != null && des.length() > 0) this.description = des;
    }
    catch (Exception x)
    {}
    try
    {
      String sel = ds.get("WollMuxSelected");
      if (sel != null
        && (sel.equalsIgnoreCase("true") || sel.equals("1") || sel.equalsIgnoreCase("ja")))
        selected = true;
    }
    catch (Exception x)
    {}
  }

  public void setSelected(boolean selected)
  {
    this.selected = selected;
  }

  public boolean isSelected()
  {
    return selected;
  }

  public Dataset getDataset()
  {
    return ds;
  }

  @Override
  public String toString()
  {
    return description;
  }
}