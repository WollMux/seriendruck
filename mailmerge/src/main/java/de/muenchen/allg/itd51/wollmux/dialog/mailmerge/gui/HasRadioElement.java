package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

import javax.swing.ButtonGroup;

/**
 * Manche UIElemente implementieren dieses Interface um damit anzuzeigen, dass sie
 * eine ButtonGroup zugeordnet werden können und einen Radio-Button enthalten,
 * dessen Vorbelegung bei Ein-/Ausblendungen durch den Seriendruckdialog verwaltet
 * werden sollen.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public interface HasRadioElement
{
  public void setButtonGroup(ButtonGroup g);

  public void setSelected(boolean b);

  public boolean isSelected();
}