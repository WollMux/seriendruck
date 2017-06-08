package de.muenchen.mailmerge.dialog.mailmerge.gui;

/**
 * Beschreibt die Ausrichtung, nach der Formularelemente innerhalb einer
 * {@link Section} ausgerichtet werden können.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
enum Orientation {
  vertical,
  horizontal;

  static Orientation getByname(String s)
  {
    for (Orientation o : Orientation.values())
    {
      if (o.toString().equalsIgnoreCase(s))
        return o;
    }
    return vertical;
  }
}