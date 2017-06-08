/*
 * Dateiname: DruckerModel.java
 * Projekt  : WollMux
 * Funktion : Das Model für den Druckerauswahl Dialog beim Seriendruck.
 *            Siehe auch DruckerController und DruckerAuswahlDialog.
 *
 * Copyright (c) 2014-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 23.01.2014 | loi | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Judith Baur, Simona Loi
 * @version 1.0
 *
 */
package de.muenchen.mailmerge.dialog.mailmerge;

import java.util.ArrayList;
import java.util.List;

public class DruckerModel {

  String selectedPrinter;
  ArrayList<String> alleDrucker = new ArrayList<>();

  public void setAlleDrucker(List<String> listeDerDrucker) {
    alleDrucker = new ArrayList<>(listeDerDrucker);
  }

  public List<String> getAlleDrucker() {
    return alleDrucker;
  }

  public void setDrucker(String drucker) {
    selectedPrinter = drucker;
  }

  public String getDrucker() {
    return selectedPrinter;
  }
}
