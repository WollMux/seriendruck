package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

/**
 * Beschreibt Elementtypen, wie sie im TYPE-Attribut von Einträgen des
 * Seriendruckdialog-Abschnitts der WollMux-Konfiguration verwendet werden können.
 * Der Seriendruckabschnitt definiert im Vergleich zur FormGUI einige
 * Spezial-Typen, die nur im Kontext dieses Dialogs einen Sinn ergeben.
 * 
 * Die Methode {@link #getByname(String)} ermöglicht eine Zuordnung von Strings der
 * Konfigurationsdatei auf den entsprechenden enum-Typen.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public enum UIElementType {
  radio,
  label,
  description,
  fromtoradio,
  targetdirpicker,
  filenametemplatechooser,
  emailfrom,
  emailtofieldname,
  emailtext,
  emailsubject,
  printersettings,
  glue,
  button,
  unknown;

  public static UIElementType getByname(String s)
  {
    for (UIElementType t : UIElementType.values())
    {
      if (t.toString().equalsIgnoreCase(s)) return t;
    }
    return unknown;
  }
}