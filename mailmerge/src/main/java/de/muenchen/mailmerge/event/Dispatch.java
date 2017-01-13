/*
 * Dateiname: Dispatch.java
 * Projekt  : WollMux
 * Funktion : Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die kein DocumentModel erfordern.
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
 * 05.11.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.mailmerge.event;

/**
 * Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die kein
 * DocumentModel erfordern. Nähere Infos zur Funktionsweise siehe
 * {@link BaseDispatch}.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class Dispatch extends BaseDispatch
{
  public static final String DISP_unoPrint = ".uno:Print";

  public static final String DISP_unoPrintDefault = ".uno:PrintDefault";

  public static final String DISP_unoPrinterSetup = ".uno:PrinterSetup";

  public static final String DISP_wmAbsenderAuswaehlen =
    "wollmux:AbsenderAuswaehlen";

  public static final String DISP_wmPALVerwalten = "wollmux:PALVerwalten";

  public static final String DISP_wmSeriendruck = "mailmerge:Seriendruck";
}
