/*
 * Dateiname: PrintModelFactory.java
 * Projekt  : WollMux
 * Funktion : Diese Klasse enthält eine Fabrik für die Erzeugung eines PrintModels
 *            und die Klassendefinitionen des MasterPrintModels und des SlavePrintModels.
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 01.10.2007 | LUT | Erstellung als PrintModelFactory
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.print.model;

import com.sun.star.lang.NoSuchMethodException;

import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Diese Klasse enthält eine Fabrik für die Erzeugung eines XPrintModels, die
 * Klassendefinitionen des MasterPrintModels und des SlavePrintModels, mit deren
 * Hilfe die Verkettung mehrerer PrintFunctions möglich ist. Ein XPrintModel hält
 * alle Daten und Methoden bereit, die beim Drucken aus einer Druckfunktion heraus
 * benötigt werden.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class PrintModels
{
  /**
   * Spezial-Property für {@link XPrintModel#setPropertyValue(String, Object)} zum
   * Setzen eines Beschreibungsstrings des aktuell in Bearbeitung befindlichen
   * Druckvorgangs. Für die Verwendung des Stage-Feature innerhalb des WollMux siehe
   * {@link #setStage(XPrintModel, String)}.
   */
  static final String STAGE = "STAGE";

  /**
   * Erzeugt ein PrintModel-Objekt, das einen Druckvorgang zum Dokument
   * TextDocumentModel model repräsentiert. Pro Druckvorgang wird dabei ein neuer
   * PrintModelMaster erzeugt, der ein oder mehrere PrintModelSlaves anspricht und so
   * eine Verkettung mehrerer Druckfunktionen ermöglicht.
   * 
   * @param documentController
   *          Das Dokument das gedruckt werden soll
   * @return das neue PrintModel für diesen Druckvorgang
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static XPrintModel createPrintModel(TextDocumentController documentController)
  {
    return new MasterPrintModel(documentController);
  }
  
  /**
   * Liefert ein neues zu diesem TextDocumentModel zugehörige XPrintModel für einen
   * Druckvorgang; ist useDocumentPrintFunctions==true, so werden bereits alle im
   * Dokument gesetzten Druckfunktionen per
   * XPrintModel.usePrintFunctionWithArgument(...) hinzugeladen.
   * 
   * @param documentController
   *          Das Dokument das gedruckt werden soll
   * @param useDocPrintFunctions
   *          steuert ob das PrintModel mit den im Dokument gesetzten Druckfunktionen
   *          vorbelegt sein soll.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static XPrintModel createPrintModel(TextDocumentController documentController, boolean useDocPrintFunctions)
  {
    XPrintModel pmod = PrintModels.createPrintModel(documentController);
    if (useDocPrintFunctions)
    {
      for (String name : documentController.getModel().getPrintFunctions())
      {
        try
        {
          pmod.usePrintFunction(name);
        }
        catch (NoSuchMethodException e)
        {
          Logger.error(e);
        }
      }
    }
    return pmod;
  }



  /**
   * Setzt die Beschreibung des aktuellen Druckbearbeitungsvorgangs für das
   * XPrintModel pmod auf stage
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static void setStage(XPrintModel pmod, String stage)
  {
    if (pmod == null) return;
    try
    {
      pmod.setPropertyValue(STAGE, stage);
    }
    catch (Exception e)
    {
      Logger.error(L.m("Kann Stage nicht auf '%1' setzen", stage), e);
    }
  }
}
